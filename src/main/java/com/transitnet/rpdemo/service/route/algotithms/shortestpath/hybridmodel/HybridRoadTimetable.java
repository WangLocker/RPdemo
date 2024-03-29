package com.transitnet.rpdemo.service.route.algotithms.shortestpath.hybridmodel;

import com.transitnet.rpdemo.model.ETransportationMode;
import com.transitnet.rpdemo.model.ICoreEdge;
import com.transitnet.rpdemo.model.ICoreNode;
import com.transitnet.rpdemo.model.IPath;
import com.transitnet.rpdemo.model.transit.TransitNode;
import com.transitnet.rpdemo.service.route.algotithms.nearestneighbor.INearestNeighborComputation;
import com.transitnet.rpdemo.service.route.algotithms.shortestpath.AShortestPathComputation;
import com.transitnet.rpdemo.service.route.algotithms.shortestpath.IHasPathCost;
import com.transitnet.rpdemo.service.route.algotithms.shortestpath.IShortestPathComputation;
import com.transitnet.rpdemo.service.route.algotithms.shortestpath.TripletonPath;
import com.transitnet.rpdemo.util.NestedMap;
import com.transitnet.rpdemo.util.RoutingUtil;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shortest path computation algorithm which combines a given algorithm for a
 * road graph with an algorithm for transit timetable data.<br>
 * <br>
 * Therefore the algorithm, given source and destination in the road graph,
 * first determines access nodes from where it transitions into the transit
 * network. It then computes shortest paths from the sources and destinations to
 * their corresponding access nodes using the road algorithm and then from all
 * source to destination access nodes using the transit algorithm. Afterwards it
 * combines the shortest paths and computes the shortest of them.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 */
public final class HybridRoadTimetable extends AShortestPathComputation<ICoreNode, ICoreEdge<ICoreNode>> {
  /**
   * Converts the given time in milliseconds since epoch to seconds since
   * midnight at the given date.
   *
   * @param millis The time in milliseconds since epoch to convert
   * @return The time in seconds since midnight at the given date
   */
  private static int millisSinceEpochToSecondsSinceMidnight(final long millis) {
    final LocalDateTime dateTimeAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    return dateTimeAt.toLocalTime().toSecondOfDay();
  }

  /**
   * Object used to compute access nodes.
   */
  private final IAccessNodeComputation<ICoreNode, ICoreNode> mAccessNodeComputation;
  /**
   * Departure time to start routing at, in seconds since midnight.
   */
  private final long mDepTime;
  /**
   * The algorithm to compute shortest paths on road data, used as fallback if
   * no hybrid route was found
   */
  private final IShortestPathComputation<ICoreNode, ICoreEdge<ICoreNode>> mRoadComputationFallback;
  /**
   * The algorithm to compute shortest paths on road data, used for small
   * distances from source and destination to their access nodes
   */
  private final IShortestPathComputation<ICoreNode, ICoreEdge<ICoreNode>> mRoadComputationToAccessNodes;
  /**
   * Object to use for retrieving the nearest road node to a given stop
   */
  private final INearestNeighborComputation<ICoreNode> mStopToNearestRoadNode;
  /**
   * The algorithm to compute shortest paths on transit data.
   */
  private final IShortestPathComputation<ICoreNode, ICoreEdge<ICoreNode>> mTransitComputation;

  /**
   * Whether the algorithm should only route on the road network. Can be used to
   * improve the computation by completely ignoring transit data.
   */
  private final boolean mUseRoadOnly;

  /**
   * Creates a new hybrid road timetable algorithm.
   *
   * @param roadComputationFallback      The algorithm to compute shortest paths
   *                                     on road data, used as fallback if no
   *                                     hybrid route was found
   * @param roadComputationToAccessNodes The algorithm to compute shortest paths
   *                                     on road data, used for small distances
   *                                     from source and destination to their
   *                                     access nodes
   * @param transitComputation           The algorithm to compute shortest paths
   *                                     on transit data
   * @param accessNodeComputation        Object used to compute access nodes
   * @param stopToNearestRoadNode        Object to use for retrieving the
   *                                     nearest road node to a given stop
   * @param modes                        The allowed transportation modes
   * @param depTime                      Departure time to start routing at, in
   *                                     seconds since midnight
   */
  public HybridRoadTimetable(final IShortestPathComputation<ICoreNode, ICoreEdge<ICoreNode>> roadComputationFallback,
      final IShortestPathComputation<ICoreNode, ICoreEdge<ICoreNode>> roadComputationToAccessNodes,
      final IShortestPathComputation<ICoreNode, ICoreEdge<ICoreNode>> transitComputation,
      final IAccessNodeComputation<ICoreNode, ICoreNode> accessNodeComputation,
      final INearestNeighborComputation<ICoreNode> stopToNearestRoadNode, final Set<ETransportationMode> modes,
      final long depTime) {
    mRoadComputationFallback = roadComputationFallback;
    mRoadComputationToAccessNodes = roadComputationToAccessNodes;
    mTransitComputation = transitComputation;
    mAccessNodeComputation = accessNodeComputation;
    mStopToNearestRoadNode = stopToNearestRoadNode;
    mUseRoadOnly = !modes.contains(ETransportationMode.TRAM);
    mDepTime = depTime;
  }

  @Override
  public Collection<ICoreNode> computeSearchSpace(final Collection<ICoreNode> sources, final ICoreNode destination) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<IPath<ICoreNode, ICoreEdge<ICoreNode>>> computeShortestPath(final Collection<ICoreNode> sources,
                                                                              final ICoreNode destination) {
    final Optional<IPath<ICoreNode, ICoreEdge<ICoreNode>>> roadOnlyPath =
        mRoadComputationFallback.computeShortestPath(sources, destination);
    if (mUseRoadOnly) {
      return roadOnlyPath;
    }

    final Map<ICoreNode, Collection<ICoreNode>> sourceToAccessNodes =
        sources.stream().collect(Collectors.toMap(Function.identity(), mAccessNodeComputation::computeAccessNodes));
    final Collection<ICoreNode> destinationAccessNodes = mAccessNodeComputation.computeAccessNodes(destination);

    final Map<ICoreNode, IPath<ICoreNode, ICoreEdge<ICoreNode>>> shortestPathToSourceAccess = new HashMap<>();
    final Map<ICoreNode, IPath<ICoreNode, ICoreEdge<ICoreNode>>> destinationAccessPaths = new HashMap<>();
    final Set<ICoreNode> reachableSourceAccessNodes = new HashSet<>();
    final Set<ICoreNode> reachableDestinationAccessNodes = new HashSet<>();

    // Paths from source to access nodes
    for (final Entry<ICoreNode, Collection<ICoreNode>> sourceWithAccessNodes : sourceToAccessNodes.entrySet()) {
      final ICoreNode source = sourceWithAccessNodes.getKey();
      for (final ICoreNode accessNode : sourceWithAccessNodes.getValue()) {
        final Optional<ICoreNode> roadRepresentative = mStopToNearestRoadNode.getNearestNeighbor(accessNode);
        if (!roadRepresentative.isPresent()) {
          continue;
        }
        final Optional<IPath<ICoreNode, ICoreEdge<ICoreNode>>> path =
            mRoadComputationToAccessNodes.computeShortestPath(source, roadRepresentative.get());
        if (!path.isPresent()) {
          continue;
        }

        reachableSourceAccessNodes.add(accessNode);
        final IPath<ICoreNode, ICoreEdge<ICoreNode>> currentShortestPathToAccess =
            shortestPathToSourceAccess.get(accessNode);
        if (currentShortestPathToAccess == null
            || path.get().getTotalCost() < currentShortestPathToAccess.getTotalCost()) {
          shortestPathToSourceAccess.put(accessNode, path.get());
        }
      }
    }

    if (reachableSourceAccessNodes.isEmpty()) {
      return roadOnlyPath;
    }

    // Paths from destination access nodes to destination
    for (final ICoreNode destinationAccess : destinationAccessNodes) {
      final Optional<ICoreNode> roadRepresentative = mStopToNearestRoadNode.getNearestNeighbor(destinationAccess);
      if (!roadRepresentative.isPresent()) {
        continue;
      }
      final Optional<IPath<ICoreNode, ICoreEdge<ICoreNode>>> path =
          mRoadComputationToAccessNodes.computeShortestPath(roadRepresentative.get(), destination);
      if (!path.isPresent()) {
        continue;
      }
      reachableDestinationAccessNodes.add(destinationAccess);
      destinationAccessPaths.put(destinationAccess, path.get());
    }

    if (reachableDestinationAccessNodes.isEmpty()) {
      return roadOnlyPath;
    }

    // Search space from source access nodes to destination access nodes
    boolean isThereATransitPath = false;
    final NestedMap<ICoreNode, ICoreNode, IPath<ICoreNode, ICoreEdge<ICoreNode>>> transitPaths = new NestedMap<>();
    for (final ICoreNode sourceAccess : reachableSourceAccessNodes) {
      // Create transit query nodes from the access nodes
      final long duration =
          (long) Math.ceil(RoutingUtil.secondsToMillis(shortestPathToSourceAccess.get(sourceAccess).getTotalCost()));
      final long depTimeAtAccess = mDepTime + duration;
      final TransitNode sourceAccessQuery = new TransitNode(sourceAccess.getId(), sourceAccess.getLatitude(),
          sourceAccess.getLongitude(), HybridRoadTimetable.millisSinceEpochToSecondsSinceMidnight(depTimeAtAccess));

      for (final ICoreNode destinationAccess : reachableDestinationAccessNodes) {
        final Optional<IPath<ICoreNode, ICoreEdge<ICoreNode>>> path =
            mTransitComputation.computeShortestPath(sourceAccessQuery, destinationAccess);
        if (!path.isPresent()) {
          continue;
        }
        isThereATransitPath = true;
        transitPaths.put(sourceAccess, destinationAccess, path.get());
      }
    }

    if (!isThereATransitPath) {
      return roadOnlyPath;
    }

    // Construct paths and choose shortest
    IPath<ICoreNode, ICoreEdge<ICoreNode>> shortestPath = roadOnlyPath.orElseGet(() -> null);
    for (final ICoreNode sourceAccess : transitPaths.keySet()) {
      final IPath<ICoreNode, ICoreEdge<ICoreNode>> sourceToAccess = shortestPathToSourceAccess.get(sourceAccess);

      final Map<ICoreNode, IPath<ICoreNode, ICoreEdge<ICoreNode>>> destinationAccessToPath =
          transitPaths.get(sourceAccess);
      for (final ICoreNode destinationAccess : destinationAccessToPath.keySet()) {
        final IPath<ICoreNode, ICoreEdge<ICoreNode>> sourceAccessToDestinationAccess =
            destinationAccessToPath.get(destinationAccess);
        final IPath<ICoreNode, ICoreEdge<ICoreNode>> accessToDestination =
            destinationAccessPaths.get(destinationAccess);

        final IPath<ICoreNode, ICoreEdge<ICoreNode>> path =
            new TripletonPath<>(sourceToAccess, sourceAccessToDestinationAccess, accessToDestination);
        if (shortestPath == null || path.getTotalCost() < shortestPath.getTotalCost()) {
          shortestPath = path;
        }
      }
    }

    return Optional.ofNullable(shortestPath);
  }

  @Override
  public Optional<Double> computeShortestPathCost(final Collection<ICoreNode> sources, final ICoreNode destination) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<ICoreNode, ? extends IHasPathCost> computeShortestPathCostsReachable(final Collection<ICoreNode> sources) {
    throw new UnsupportedOperationException();
  }

}

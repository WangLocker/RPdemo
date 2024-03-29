package com.transitnet.rpdemo.service.route.algotithms.shortestpath.connectionscan;

import com.transitnet.rpdemo.model.*;
import com.transitnet.rpdemo.model.timetable.Connection;
import com.transitnet.rpdemo.model.timetable.Stop;
import com.transitnet.rpdemo.model.timetable.Timetable;
import com.transitnet.rpdemo.model.timetable.Trip;
import com.transitnet.rpdemo.model.transit.IHasTime;
import com.transitnet.rpdemo.model.transit.TransitEdge;
import com.transitnet.rpdemo.model.transit.TransitNode;
import com.transitnet.rpdemo.service.route.algotithms.shortestpath.AShortestPathComputation;
import com.transitnet.rpdemo.service.route.algotithms.shortestpath.EdgePath;
import com.transitnet.rpdemo.service.route.algotithms.shortestpath.IHasPathCost;
import com.transitnet.rpdemo.service.route.algotithms.shortestpath.PathCost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Implementation of the Connection-Scan algorithm that is able to compute
 * shortest paths on a given timetable. A timetable represents a transit
 * network.<br>
 * <br>
 * For details refer to:
 * <ul>
 * <li><code>Connection Scan Algorithm</code> - Dibbelt J., Pajor T., Strasser B.
 * and Wagner D. - 2017 -
 * <a href="https://arxiv.org/abs/1703.05997">arxiv.org/abs/1703.05997</a></li>
 * </ul>
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 */
public final class ConnectionScan extends AShortestPathComputation<ICoreNode, ICoreEdge<ICoreNode>> {
  /**
   * Logger used for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionScan.class);
  /**
   * Amount of seconds of a day.
   */
  private static final int SECONDS_OF_DAY = 24 * 60 * 60;

  /**
   * Creates and adds an edge from the given source to destination to the given
   * path. The cost of the edge is determined by the time difference of both
   * nodes.
   *
   * @param path        The path to add the edge to
   * @param source      The source node of the edge
   * @param destination The destination node of the edge
   * @param walkByFoot  <code>True</code> if the transportation mode of the edge is
   *                    by foot, <code>false</code> if by tram.
   */
  private static void addEdgeToPath(final EdgePath<ICoreNode, ICoreEdge<ICoreNode>> path, final TransitNode source,
                                    final TransitNode destination, final boolean walkByFoot) {
    final double cost = destination.getTime() - source.getTime();
    final ICoreEdge<ICoreNode> edge;
    if (walkByFoot) {
      edge = new FootpathTransitEdge<>(0, source, destination, cost);
    } else {
      edge = new TransitEdge<>(0, source, destination, cost);
    }
    path.addEdge(edge, cost);
  }

  /**
   * Computes the duration between given departure and arrival times.
   *
   * @param depTime The departure time in seconds since midnight.
   * @param arrTime The arrival time in seconds since midnight
   * @return The duration between given departure and arrival, in seconds
   * @throws IllegalArgumentException If the departure time is greater than the
   *                                  arrival time
   */
  private static double computeDuration(final int depTime, final int arrTime) throws IllegalArgumentException {
    if (depTime > arrTime) {
      throw new IllegalArgumentException();
    }

    return arrTime - depTime;
  }

  /**
   * Extracts the time from the given node.
   *
   * @param node The node to extract the time from
   * @return The extracted time
   * @throws IllegalArgumentException If the given node has no time
   */
  private static final int extractStartingTime(final ICoreNode node) throws IllegalArgumentException {
    if (!(node instanceof IHasTime)) {
      throw new IllegalArgumentException();
    }
    return ((IHasTime) node).getTime();
  }

  /**
   * Validates the given time against the threshold. If the time is before the
   * threshold it is increased by one day. By that it is ensured that the result
   * is always after the threshold.
   *
   * @param time      The time to validate in seconds since midnight
   * @param threshold The threshold to validate against in seconds since
   *                  midnight
   * @return The validated time in seconds since midnight. Either the given
   *         argument, if it was already after the threshold, or shifted by the
   *         amount of seconds of one whole day.
   */
  private static int validateTimeBeforeAfter(final int time, final int threshold) {
    if (time < threshold) {
      return time + SECONDS_OF_DAY;
    }
    return time;
  }

  /**
   * The timetable data to route on.
   */
  private final Timetable mTable;

  /**
   * Creates a new connection scan algorithm.
   *
   * @param table The timetable data to route on
   */
  public ConnectionScan(final Timetable table) {
    mTable = table;
  }

  @Override
  public Collection<ICoreNode> computeSearchSpace(final Collection<ICoreNode> sources, final ICoreNode destination) {
    final int startingTime = ConnectionScan.extractStartingTime(sources.iterator().next());
    final ConnectionScanResult result = computeShortestPathHelper(sources, destination, startingTime);

    // Collect all visited stops
    final Collection<ICoreNode> searchSpace = new ArrayList<>();
    final int[] stopToArrTime = result.getStopToArrTime();
    for (int i = 0; i < stopToArrTime.length; i++) {
      final int arrTime = stopToArrTime[i];
      // Skip if not visited
      if (arrTime == Integer.MAX_VALUE) {
        continue;
      }
      final Stop stop = mTable.getStop(i);
      searchSpace.add(new TransitNode(i, stop.getLatitude(), stop.getLongitude(), arrTime));
    }

    return searchSpace;
  }

  @Override
  public Optional<IPath<ICoreNode, ICoreEdge<ICoreNode>>> computeShortestPath(final Collection<ICoreNode> sources,
                                                                              final ICoreNode destination) {
    final int startingTime = ConnectionScan.extractStartingTime(sources.iterator().next());
    final ConnectionScanResult result = computeShortestPathHelper(sources, destination, startingTime);
    final int[] stopToArrTime = result.getStopToArrTime();

    // Not reachable
    if (stopToArrTime[destination.getId()] == Integer.MAX_VALUE) {
      return Optional.empty();
    }

    // Construct path
    final JourneyPointer[] stopToJourney = result.getStopToJourney();
    final EdgePath<ICoreNode, ICoreEdge<ICoreNode>> path = new EdgePath<>(true);
    int currentStopId = destination.getId();
    TransitNode currentDestination = createNodeForStop(currentStopId, stopToArrTime[currentStopId]);

    // Backtrack journey pointers from destination to source. Stop when the
    // initial pointer was found, i.e. a pointer only containing an initial
    // footpath.
    // TODO CSA is likely to have a bug, sometimes the pointer induce
    // a loop which should not be possible. Remove the loop detection after
    // investigating the issue. Current guess: Induced by footpaths not obeying
    // the triangle inequality (i.e. its cheaper to visit an already visited
    // stop again for a cheap footpath than using a direct footpath). This was
    // fixed in the current version. Check if the issue remains.
    final Set<Integer> visitedStopsLoopDetection = new LinkedHashSet<>();
    while (stopToJourney[currentStopId].getEnterConnection() != null) {
      // TODO Loop detection from here ...
      if (visitedStopsLoopDetection.contains(currentStopId)) {
        // Loop detected
        final Path dumpPath = Paths.get("bugDump.dmp");
        LOGGER.info("Bug: Detected a loop, aborting computation and returning empty path.");
        LOGGER.info("Bug data dumped to: " + dumpPath.toAbsolutePath());
        final List<String> dumpLines = new ArrayList<>();
        dumpLines.add("#-----------------------------------------------------------------");
        dumpLines.add("#Bug dump, detected a loop in CSA path extraction.");
        dumpLines.add("#Query from " + sources + " to " + destination + " with depTime at " + startingTime);
        dumpLines.add(
            "#Visited stops in extraction " + visitedStopsLoopDetection + ", visiting " + currentStopId + " again");
        dumpLines.add("#Relevant journey pointers:");
        for (final int visitedStop : visitedStopsLoopDetection) {
          dumpLines.add("\t" + visitedStop + " -> " + stopToJourney[visitedStop]);
        }
        dumpLines.add("\t" + currentStopId + " -> " + stopToJourney[currentStopId]);
        dumpLines.add("#Complete journey pointer dump:");
        for (int i = 0; i < stopToJourney.length; i++) {
          dumpLines.add("\t" + i + " -> " + stopToJourney[i]);
        }
        try {
          Files.write(dumpPath, dumpLines, StandardOpenOption.CREATE, StandardOpenOption.APPEND,
              StandardOpenOption.WRITE);
        } catch (final IOException e) {
          e.printStackTrace();
        }
        return Optional.empty();
      }
      visitedStopsLoopDetection.add(currentStopId);
      // TODO ... to here

      final JourneyPointer pointer = stopToJourney[currentStopId];
      final Trip trip = mTable.getTrip(pointer.getExitConnection().getTripId());
      final Connection exitConnection = pointer.getExitConnection();
      final Connection enterConnection = pointer.getEnterConnection();

      // Departure of footpath, arrival of trip exit
      final TransitNode tripPartArr = createNodeForStop(exitConnection.getArrStopId(),
          ConnectionScan.validateTimeBeforeAfter(exitConnection.getArrTime(), startingTime));
      ConnectionScan.addEdgeToPath(path, tripPartArr, currentDestination, true);

      // Add the trip
      TransitNode currentConnectionArr = tripPartArr;
      final int exitIndex = exitConnection.getSequenceIndex();
      final int enterIndex = enterConnection.getSequenceIndex();
      // Traverse the used part of the sequence reversely
      for (int i = exitIndex; i >= enterIndex; i--) {
        final Connection connection = trip.getConnectionAtSequenceIndex(i);

        final TransitNode connectionDep = createNodeForStop(connection.getDepStopId(),
            ConnectionScan.validateTimeBeforeAfter(connection.getDepTime(), startingTime));
        ConnectionScan.addEdgeToPath(path, connectionDep, currentConnectionArr, false);

        // Prepare next connection of the trip
        currentConnectionArr = connectionDep;
      }

      // Prepare next journey pointer
      currentStopId = enterConnection.getDepStopId();
      currentDestination = currentConnectionArr;
    }

    // Add the initial footpath from the source to the first connection. This
    // also handles the special case were the shortest path only consists of a
    // direct footpath between the source and destination.
    final Footpath initialFootpath = stopToJourney[currentStopId].getFootpath();
    final TransitNode sourceNode = createNodeForStop(initialFootpath.getDepStopId(), startingTime);
    ConnectionScan.addEdgeToPath(path, sourceNode, currentDestination, true);

    return Optional.of(path);
  }

  @Override
  public Optional<Double> computeShortestPathCost(final Collection<ICoreNode> sources, final ICoreNode destination) {
    final int startingTime = ConnectionScan.extractStartingTime(sources.iterator().next());
    final ConnectionScanResult result = computeShortestPathHelper(sources, destination, startingTime);

    final int arrTime = result.getStopToArrTime()[destination.getId()];

    // Not reachable
    if (arrTime == Integer.MAX_VALUE) {
      return Optional.empty();
    }

    return Optional.of(ConnectionScan.computeDuration(startingTime, arrTime));
  }

  @Override
  public Map<ICoreNode, ? extends IHasPathCost> computeShortestPathCostsReachable(final Collection<ICoreNode> sources) {

    final int startingTime = ConnectionScan.extractStartingTime(sources.iterator().next());
    final ConnectionScanResult result = computeShortestPathHelper(sources, null, startingTime);

    // Collect all reachable stops
    final Map<ICoreNode, PathCost> stopToCost = new HashMap<>();
    final int[] stopToArrTime = result.getStopToArrTime();
    for (int i = 0; i < stopToArrTime.length; i++) {
      final int arrTime = stopToArrTime[i];
      // Skip if not reachable
      if (arrTime == Integer.MAX_VALUE) {
        continue;
      }

      final PathCost cost = new PathCost(ConnectionScan.computeDuration(startingTime, arrTime));
      stopToCost.put(createNodeForStop(i, arrTime), cost);
    }

    return stopToCost;
  }

  /**
   * Helper method to compute shortest paths from the given sources to a
   * possible destination.
   *
   * @param sources         The sources to start computation from, must not be
   *                        empty.
   * @param pathDestination The destination to route to or <code>null</code> if
   *                        routing to all reachable stops is desired
   * @param startingTime    The time to start routing at in seconds since
   *                        midnight
   * @return An object containing the results of the algorithm
   */
  private ConnectionScanResult computeShortestPathHelper(final Collection<ICoreNode> sources,
      final ICoreNode pathDestination, final int startingTime) {
    final Integer destinationStop;
    if (pathDestination == null) {
      destinationStop = null;
    } else {
      destinationStop = pathDestination.getId();
    }

    // Initialize data-structures
    final int[] stopToTentativeArrTime = new int[mTable.getGreatestStopId() + 1];
    Arrays.fill(stopToTentativeArrTime, Integer.MAX_VALUE);
    final Connection[] tripToEarliestReachableConnection = new Connection[mTable.getGreatestTripId() + 1];
    final JourneyPointer[] stopToJourney = new JourneyPointer[mTable.getGreatestStopId() + 1];

    // Relax all initial footpaths
    sources.stream().map(IHasId::getId).flatMap(mTable::getOutgoingFootpaths).forEach(footpath -> {
      // Only use footpath if it improves the arrival time at the destination
      final int footpathArrStopId = footpath.getArrStopId();
      final int footpathTime = startingTime + footpath.getDuration();
      if (footpathTime >= stopToTentativeArrTime[footpathArrStopId]) {
        return;
      }
      stopToTentativeArrTime[footpathArrStopId] = footpathTime;
      // Add an initial footpath as journey pointer
      stopToJourney[footpathArrStopId] = new JourneyPointer(null, null, footpath);
    });

    // Process all connections ordered starting from the first after the
    // starting time
    final Iterator<Connection> connections = mTable.getConnectionsStartingSince(startingTime);
    while (connections.hasNext()) {
      final Connection connection = connections.next();
      final int depTime = ConnectionScan.validateTimeBeforeAfter(connection.getDepTime(), startingTime);
      final int depStopId = connection.getDepStopId();
      final int arrTime = ConnectionScan.validateTimeBeforeAfter(connection.getArrTime(), startingTime);
      final int arrStopId = connection.getArrStopId();
      final int tripId = connection.getTripId();

      // Arrived at destination before this connection. The connection can thus
      // not improve the time anymore and since connections are processed
      // ordered the algorithm has finished.
      if (destinationStop != null && stopToTentativeArrTime[destinationStop] <= depTime) {
        break;
      }

      if (tripToEarliestReachableConnection[tripId] == null) {
        // Only process connections that can be taken due to a previous arrival
        // at the departure stop before the departure time
        if (stopToTentativeArrTime[depStopId] > depTime) {
          continue;
        }

        // Trip is used for the first time
        tripToEarliestReachableConnection[tripId] = connection;
      }

      // Do not relax if connection does not improve arrival time at this stop
      if (arrTime >= stopToTentativeArrTime[arrStopId]) {
        continue;
      }

      // Relax all outgoing footpaths
      mTable.getOutgoingFootpaths(arrStopId).forEach(footpath -> {
        final int footpathArrStopId = footpath.getArrStopId();
        final int footpathTime = arrTime + footpath.getDuration();

        // Only use footpath if it improves the arrival time at the destination
        if (footpathTime >= stopToTentativeArrTime[footpathArrStopId]) {
          return;
        }

        // Take this footpath
        stopToTentativeArrTime[footpathArrStopId] = footpathTime;
        stopToJourney[footpathArrStopId] =
            new JourneyPointer(tripToEarliestReachableConnection[tripId], connection, footpath);
      });
    }

    return new ConnectionScanResult(stopToTentativeArrTime, stopToJourney);
  }

  /**
   * Creates and returns a node for the given stop at the given time.
   *
   * @param stopId The ID of the stop to create a node for
   * @param time   The time at the stop to create a node for
   * @return The created node
   */
  private TransitNode createNodeForStop(final int stopId, final int time) {
    final Stop stop = mTable.getStop(stopId);
    return new TransitNode(stopId, stop.getLatitude(), stop.getLongitude(), time);
  }
}

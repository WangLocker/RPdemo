package com.transitnet.rpdemo.service.route.algotithms.shortestpath.dijkstra.modules;

import com.transitnet.rpdemo.model.IEdge;
import com.transitnet.rpdemo.model.INode;
import com.transitnet.rpdemo.service.route.algotithms.shortestpath.dijkstra.TentativeDistance;


import java.util.OptionalDouble;

/**
 * Interface for Dijkstra modules used by {@link ModuleDijkstra}. Defines
 * various methods that allow to manipulate how the base Dijkstra works, like
 * providing edge costs different to {@link IEdge#getCost()}.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 * @param <N> Type of the nodes
 * @param <E> Type of the edges
 */
public interface IModule<N extends INode, E extends IEdge<N>> {
  /**
   * Whether or not the given edge should be considered for relaxation. The
   * algorithm will ignore the edge and not follow it if this method returns
   * <code>false</code>.
   *
   * @param edge            The edge in question
   * @param pathDestination The destination of the shortest path computation or
   *                        <code>null</code> if not present
   * @return <code>True</code> if the edge should be considered, <code>false</code>
   *         otherwise
   */
  default boolean considerEdgeForRelaxation(@SuppressWarnings("unused") final E edge,
      @SuppressWarnings("unused") final N pathDestination) {
    return true;
  }

  /**
   * Gets an estimate about the shortest path distance from the given node to
   * the destination of the shortest path computation.<br>
   * <br>
   * The estimate must be <i>monotone</i> and <i>admissible</i>.
   *
   * @param node            The node to estimate the distance from
   * @param pathDestination The destination to estimate the distance to
   * @return An estimate about the shortest path distance or empty if the module
   *         has no estimate
   */
  default OptionalDouble getEstimatedDistance(@SuppressWarnings("unused") final N node,
      @SuppressWarnings("unused") final N pathDestination) {
    return OptionalDouble.empty();
  }

  /**
   * Provides the cost of a given edge.<br>
   * <br>
   * The base is the result of {@link IEdge#getCost()}. Implementations are
   * allowed to override this method in order to modify the cost.
   *
   * @param edge              The edge whose cost to provide
   * @param tentativeDistance The current tentative distance when relaxing this
   *                          edge
   * @return The cost of the given edge or empty if the module does not provide
   *         a cost
   */
  default OptionalDouble provideEdgeCost(@SuppressWarnings("unused") final E edge,
      @SuppressWarnings("unused") final double tentativeDistance) {
    return OptionalDouble.empty();
  }

  /**
   * Whether or not the algorithm should abort computation of the shortest path.
   * The method is called right after the given node has been settled.
   *
   * @param tentativeDistance The tentative distance wrapper of the node that
   *                          was settled
   * @return <code>True</code> if the computation should be aborted, <code>false</code>
   *         if not
   */
  default boolean shouldAbort(@SuppressWarnings("unused") final TentativeDistance<N, E> tentativeDistance) {
    return false;
  }
}

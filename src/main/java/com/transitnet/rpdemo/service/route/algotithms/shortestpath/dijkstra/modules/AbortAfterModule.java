package com.transitnet.rpdemo.service.route.algotithms.shortestpath.dijkstra.modules;

import com.transitnet.rpdemo.model.IEdge;
import com.transitnet.rpdemo.model.INode;
import com.transitnet.rpdemo.service.route.algotithms.shortestpath.dijkstra.TentativeDistance;


/**
 * Module for a {@link ModuleDijkstra} that aborts computation of the shortest
 * path after exploring to a given range.<br>
 * <br>
 * The factory method {@link #of(double)} can be used for convenient instance
 * creation.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 * @param <N> Type of the nodes
 * @param <E> Type of the edges
 */
public final class AbortAfterModule<N extends INode, E extends IEdge<N>> implements IModule<N, E> {

  /**
   * Creates an module which aborts computation after exploring to the given
   * range.
   *
   * @param       <N> Type of the nodes
   * @param       <E> Type of the edges
   * @param range The range after which to abort, in travel time measured in
   *              <code>seconds</code>
   * @return The created module
   */
  public static <N extends INode, E extends IEdge<N>> AbortAfterModule<N, E> of(final double range) {
    return new AbortAfterModule<>(range);
  }

  /**
   * The range after which to abort, in travel time measured in
   * <code>seconds</code>.
   */
  private final double mRange;

  /**
   * Creates an module which aborts computation after exploring to the given
   * range.
   *
   * @param range The range after which to abort, in travel time measured in
   *              <code>seconds</code>
   */
  public AbortAfterModule(final double range) {
    mRange = range;
  }

  @Override
  public boolean shouldAbort(final TentativeDistance<N, E> tentativeDistance) {
    return tentativeDistance.getTentativeDistance() > mRange;
  }

}

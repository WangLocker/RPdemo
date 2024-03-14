package com.transitnet.rpdemo.service.route.algotithms.shortestpath;

import com.transitnet.rpdemo.model.EdgeCost;
import com.transitnet.rpdemo.model.IEdge;
import com.transitnet.rpdemo.model.INode;
import com.transitnet.rpdemo.model.IPath;
import com.transitnet.rpdemo.util.TripletonIterator;


import java.util.Iterator;

/**
 * A path wrapping three given paths.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 * @param <N> The type of the nodes
 * @param <E> The type of the edges
 */
public final class TripletonPath<N extends INode, E extends IEdge<N>> implements IPath<N, E> {
  /**
   * The first path.
   */
  private final IPath<N, E> mFirst;
  /**
   * The second path.
   */
  private final IPath<N, E> mSecond;
  /**
   * The third path.
   */
  private final IPath<N, E> mThird;

  /**
   * Creates a new path wrapping the three given paths. The resulting path
   * consists of concatenating them in order.
   *
   * @param first  The first path
   * @param second The second path
   * @param third  The third path
   */
  public TripletonPath(final IPath<N, E> first, final IPath<N, E> second, final IPath<N, E> third) {
    mFirst = first;
    mSecond = second;
    mThird = third;
  }

  @Override
  public N getDestination() {
    return mThird.getDestination();
  }

  @Override
  public N getSource() {
    return mFirst.getSource();
  }

  @Override
  public double getTotalCost() {
    return mFirst.getTotalCost() + mSecond.getTotalCost() + mThird.getTotalCost();
  }

  @Override
  public Iterator<EdgeCost<N, E>> iterator() {
    return new TripletonIterator<>(mFirst.iterator(), mSecond.iterator(), mThird.iterator());
  }

  @Override
  public int length() {
    return mFirst.length() + mSecond.length() + mThird.length();
  }

}

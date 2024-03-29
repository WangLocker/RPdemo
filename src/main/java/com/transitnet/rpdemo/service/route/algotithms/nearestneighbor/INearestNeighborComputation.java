package com.transitnet.rpdemo.service.route.algotithms.nearestneighbor;

import com.transitnet.rpdemo.model.ISpatial;

import java.util.Collection;
import java.util.Optional;

/**
 * Interface for algorithms that solve nearest neighbor computation problems.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 * @param <E> Type of the edges which must offer spatial data
 */
public interface INearestNeighborComputation<E extends ISpatial> {
  /**
   * Gets the <code>k</code> neighbors nearest to the given point. That are the
   * <code>k</code> elements closest to the given point.
   *
   * @param point The point in question
   * @param k     The amount of neighbors to get, a value of <code>0</code> yields
   *              an empty result
   * @return The <code>k</code> neighbors nearest to the given point, ascending in
   *         distance to the point
   */
  Collection<E> getKNearestNeighbors(E point, int k);

  /**
   * Gets the neighbor nearest to the given point. That is the element closest
   * to the given point.
   *
   * @param point The point in question
   * @return The neighbor nearest to the given point or <code>empty</code> if there
   *         is no
   */
  Optional<E> getNearestNeighbor(E point);

  /**
   * Gets the neighborhood of the given point with the given range. That are all
   * elements within the given range to the given point. I.e. all elements
   * inside the ball around the point with the given range as radius.
   *
   * @param point The point in question
   * @param range The range around the point, inclusive
   * @return All elements inside the ball around the point with the given range
   *         as radius
   */
  Collection<E> getNeighborhood(E point, double range);
}

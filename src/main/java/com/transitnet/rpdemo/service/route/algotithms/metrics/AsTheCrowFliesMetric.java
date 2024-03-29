package com.transitnet.rpdemo.service.route.algotithms.metrics;

import com.transitnet.rpdemo.model.ISpatial;
import com.transitnet.rpdemo.util.RoutingUtil;

/**
 * Implements the <i>as-the-crow-flies</i> metric for {@link ISpatial}
 * objects.<br>
 * <br>
 * Given two objects it computes the direct, straight-line, distance of both
 * objects based on their coordinates. The distance is measured as travel time
 * in <code>seconds</code>.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 * @param <N> The type of objects the metric operates on, must implement
 *        {@link ISpatial}
 */
public final class AsTheCrowFliesMetric<N extends ISpatial> implements IMetric<N> {

  /**
   * The distance between both given objects, measured as travel time in
   * <code>seconds</code>.
   */
  @Override
  public double distance(final N first, final N second) {
    final double distance = RoutingUtil.distanceEquiRect(first, second);
    final double maximalSpeed = RoutingUtil.maximalRoadSpeed();
    return RoutingUtil.travelTime(distance, maximalSpeed);
  }

}

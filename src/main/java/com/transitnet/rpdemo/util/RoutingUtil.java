package com.transitnet.rpdemo.util;

import com.transitnet.rpdemo.service.parse.osm.EHighwayType;
import com.transitnet.rpdemo.model.ETransportationMode;
import com.transitnet.rpdemo.model.ISpatial;

import java.util.EnumSet;
import java.util.Set;

/**
 * Utility class which offers methods related to routing.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 */
public final class RoutingUtil {
  /**
   * The mean of the earth radius in metres.
   */
  private static final int EARTH_RADIUS_MEAN = 6_371_000;
  /**
   * The amount of degrees of a half circle.
   */
  private static final int HALF_CIRCLE_DEG = 180;
  /**
   * Maximal speed of a bike in <code>km/h</code>.
   */
  private static final int MAX_BIKE_SPEED = 14;
  /**
   * Maximal walking speed in <code>km/h</code>.
   */
  private static final int MAX_FOOT_SPEED = 5;
  /**
   * The maximal possible speed on a road in <code>km/h</code>
   */
  private static final double MAXIMAL_ROAD_SPEED = 200.0;
  /**
   * Factor to multiply with to convert milliseconds to nanoseconds.
   */
  private static final int MILLIS_TO_NANO = 1_000_000;
  /**
   * Factor to multiply with to convert <code>mph</code> (miles per hour) into
   * <code>km/h</code> (kilometres per hour).
   */
  private static final double MPH_TO_KMH = 1.60934;

  /**
   * Factor to multiply with to convert <code>m/s</code> (metres per second) into
   * <code>km/h</code> (kilometres per hour).
   */
  private static final double MS_TO_KMH = 3.6;

  /**
   * Factor to multiply with to convert seconds to milliseconds.
   */
  private static final int SECONDS_TO_MILLIS = 1_000;

  /**
   * Converts the given value in <code>degrees</code> into <code>radians</code>.
   *
   * @param deg The value in <code>degrees</code> to convert
   * @return The corresponding value in <code>radians</code>
   */
  public static double degToRad(final double deg) {
    return deg * Math.PI / HALF_CIRCLE_DEG;
  }

  /**
   * Approximates the distance between the given objects by using a model which
   * represents the earth as equirectangular projection.
   *
   * @param first  The first object
   * @param second The second object
   * @return The distance between the given objects
   */
  public static double distanceEquiRect(final ISpatial first, final ISpatial second) {
    if (first == second) {
      return 0.0;
    }

    // Convert positions to radians
    final double firstLat = RoutingUtil.degToRad(first.getLatitude());
    final double firstLong = RoutingUtil.degToRad(first.getLongitude());
    final double secondLat = RoutingUtil.degToRad(second.getLatitude());
    final double secondLong = RoutingUtil.degToRad(second.getLongitude());

    final double x = (secondLong - firstLong) * Math.cos((firstLat + secondLat) / 2);
    final double y = secondLat - firstLat;
    return Math.sqrt(x * x + y * y) * EARTH_RADIUS_MEAN;
  }

  /**
   * Gets the speed used on the given highway.
   *
   * @param type     The type of the highway
   * @param maxSpeed The maximal allowed speed on the highway or <code>-1</code> if
   *                 not present
   * @param mode     The transportation mode to use for traveling
   * @return The speed used on the highway in <code>km/h</code>
   */
  public static double getSpeedOfHighway(final EHighwayType type, final int maxSpeed, final ETransportationMode mode) {
    final int speedOnRoad;
    if (maxSpeed != -1) {
      // Use the max speed property if present
      speedOnRoad = maxSpeed;
    } else if (type != null) {
      // Use the highway type if present
      speedOnRoad = type.getAverageSpeed();
    } else {
      // Use a default speed value
      speedOnRoad = EHighwayType.RESIDENTIAL.getAverageSpeed();
    }

    // Limit the speed by the transportation modes maximal speed
    if (mode == ETransportationMode.CAR) {
      // Car is not limited, use the given road speed
      return speedOnRoad;
    } else if (mode == ETransportationMode.BIKE) {
      return Math.min(speedOnRoad, MAX_BIKE_SPEED);
    } else if (mode == ETransportationMode.FOOT) {
      return Math.min(speedOnRoad, MAX_FOOT_SPEED);
    } else {
      // Assume no limit on the transportation mode
      return speedOnRoad;
    }
  }

  /**
   * Gets a set of allowed transportation modes for the given highway type.
   *
   * @param type The type of the highway
   * @return A set of allowed transportation modes
   */
  public static Set<ETransportationMode> getTransportationModesOfHighway(final EHighwayType type) {
    if (type == EHighwayType.MOTORWAY || type == EHighwayType.MOTORWAY_LINK) {
      return EnumSet.of(ETransportationMode.CAR);
    } else if (type == EHighwayType.CYCLEWAY) {
      return EnumSet.of(ETransportationMode.BIKE);
    }
    return EnumSet.of(ETransportationMode.CAR, ETransportationMode.BIKE, ETransportationMode.FOOT);
  }

  /**
   * Gets the maximal walking speed in <code>km/h</code>.
   *
   * @return The maximal walking speed in <code>km/h</code>
   */
  public static double getWalkingSpeed() {
    return MAX_FOOT_SPEED;
  }

  /**
   * Converts the given value in <code>km/h</code> (kilometres per hour) into
   * <code>m/s</code> (metres per second).
   *
   * @param kmh The value in <code>km/h</code> to convert
   * @return The corresponding value in <code>m/s</code>
   */
  public static double kmhToMs(final double kmh) {
    return kmh / MS_TO_KMH;
  }

  /**
   * Gets the maximal possible speed on a road in <code>km/h</code>.
   *
   * @return The maximal possible speed in <code>km/h</code>
   */
  public static double maximalRoadSpeed() {
    return MAXIMAL_ROAD_SPEED;
  }

  /**
   * Converts the given value in <code>milliseconds</code> into <code>seconds</code>.
   *
   * @param millis The value in <code>milliseconds</code> to convert
   * @return The corresponding value in <code>seconds</code>
   */
  public static double millisToSeconds(final long millis) {
    return ((double) millis) / SECONDS_TO_MILLIS;
  }

  /**
   * Converts the given value in <code>mph</code> (miles per hour)<code> into
   * km/h</code> (kilometres per hour).
   *
   * @param mph The value in <code>mph</code> to convert
   * @return The corresponding value in <code>km/h</code>
   */
  public static double mphToKmh(final double mph) {
    return mph * MPH_TO_KMH;
  }

  /**
   * Converts the given value in <code>m/s</code> (metres per second)<code> into
   * km/h</code> (kilometres per hour).
   *
   * @param ms The value in <code>ms/s</code> to convert
   * @return The corresponding value in <code>km/h</code>
   */
  public static double msToKmh(final double ms) {
    return ms * MS_TO_KMH;
  }

  /**
   * Converts the given value in <code>nanoseconds</code> to <code>milliseconds</code>.
   * The result is rounded down.
   *
   * @param nanos The value in <code>nanoseconds</code> to convert
   * @return The corresponding value in <code>milliseconds</code>, rounded down
   */
  public static long nanosToMillis(final long nanos) {
    return nanos / MILLIS_TO_NANO;
  }

  /**
   * Converts the given value in <code>nanoseconds</code> to <code>seconds</code>.
   *
   * @param nanos The value in <code>nanoseconds</code> to convert
   * @return The corresponding value in <code>seconds</code>
   */
  public static double nanosToSeconds(final long nanos) {
    return ((double) nanos) / MILLIS_TO_NANO / SECONDS_TO_MILLIS;
  }

  /**
   * Converts the given value in <code>radians</code> into <code>degrees</code>.
   *
   * @param rad The value in <code>radians</code> to convert
   * @return The corresponding value in <code>degrees</code>
   */
  public static double radToDeg(final double rad) {
    return rad * HALF_CIRCLE_DEG / Math.PI;
  }

  /**
   * Converts the given value in <code>seconds</code> into <code>milliseconds</code>.
   *
   * @param seconds The value in <code>seconds</code> to convert
   * @return The corresponding value in <code>milliseconds</code>
   */
  public static double secondsToMillis(final double seconds) {
    return seconds * SECONDS_TO_MILLIS;
  }

  /**
   * Converts the given value in <code>seconds</code> to <code>nanoseconds</code>.
   *
   * @param seconds The value in <code>seconds</code> to convert
   * @return The corresponding value in <code>nanoseconds</code>
   */
  public static long secondsToNanos(final double seconds) {
    return (long) (RoutingUtil.secondsToMillis(seconds) * MILLIS_TO_NANO);
  }

  /**
   * Gets the time need to travel the given distance with the given speed.
   *
   * @param distance The distance to travel in <code>metres</code>
   * @param speed    The speed to travel with in <code>km/h</code>
   * @return The time need to travel in <code>seconds</code>
   */
  public static double travelTime(final double distance, final double speed) {
    return distance / RoutingUtil.kmhToMs(speed);
  }

  /**
   * Utility class. No implementation.
   */
  private RoutingUtil() {

  }
}

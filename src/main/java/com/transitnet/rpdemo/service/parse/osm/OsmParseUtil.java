package com.transitnet.rpdemo.service.parse.osm;

import com.transitnet.rpdemo.util.RoutingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

/**
 * Utility class that offers constants and methods related to parsing OSM
 * entities.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 */
public final class OsmParseUtil {
  /**
   * The maximal speed allowed on motorways in switzerland, in <code>km/h</code>.
   */
  public static final int CH_MOTORWAY_SPEED = 120;
  /**
   * The maximal speed allowed on rural highways in switzerland, in
   * <code>km/h</code>.
   */
  public static final int CH_RURAL_SPEED = 80;
  /**
   * The maximal speed allowed in urban areas in switzerland, in <code>km/h</code>.
   */
  public static final int CH_URBAN_SPEED = 50;
  /**
   * The maximal speed allowed on living streets in germany, in <code>km/h</code>.
   */
  public static final int DE_LIVING_STREET_SPEED = 7;
  /**
   * The maximal walking speed in germany, in <code>km/h</code>.
   */
  public static final int DE_WALK_SPEED = 7;
  /**
   * Tag name for OSM ways that contains the highway property.
   */
  public static final String HIGHWAY_TAG = "highway";
  /**
   * Tag name for OSM ways that contains the maxspeed property.
   */
  public static final String MAXSPEED_TAG = "maxspeed";
  /**
   * Tag name for OSM entities that contains the name property.
   */
  public static final String NAME_TAG = "name";
  /**
   * Tag name for OSM ways that contains the oneway property.
   */
  public static final String ONEWAY_TAG = "oneway";
  /**
   * Logger to use for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(OsmParseUtil.class);

  /**
   * Gets the highway type of the given OSM way.
   *
   * @param tagToValue Map connecting the tags of the OSM way to their values
   * @return The highway type or <code>null</code> if not present
   */
  public static EHighwayType parseHighwayType(final Map<String, String> tagToValue) {
    final String highwayText = tagToValue.get(HIGHWAY_TAG);
    if (highwayText == null) {
      return null;
    }
    return EHighwayType.fromName(highwayText);
  }

  /**
   * Gets the maximal allowed speed for the given OSM way in <code>km/h</code>.<br>
   * <br>
   * If the value is in a wrong format an average value will be returned. In
   * particular, the method will not throw any exception. But the error will be
   * logged.
   *
   * @param tagToValue Map connecting the tags of the OSM way to their values
   * @return The maximal allowed speed, an average value or <code>-1</code> if not
   *         present. The speed is in <code>km/h</code>.
   */
  public static int parseMaxSpeed(final Map<String, String> tagToValue) {
    // TODO This method should be made more abstract, using maps for the
    // exceptions and external files
    final String maxSpeedText = tagToValue.get(MAXSPEED_TAG);
    if (maxSpeedText == null) {
      return -1;
    }

    // Most common case
    if (maxSpeedText.matches("\\d+")) {
      try {
        return Integer.parseInt(maxSpeedText);
      } catch (final NumberFormatException e) {
        // Use a default value instead
        LOGGER.error("Can not parse maxspeed value: {}", maxSpeedText);
        return EHighwayType.RESIDENTIAL.getAverageSpeed();
      }
    }

    // "none" means there is no speed limit, use a default value
    if (maxSpeedText.equals("none")) {
      return EHighwayType.MOTORWAY.getAverageSpeed();
    }
    // "walk" means the speed limit is at which humans tend to walk
    if (maxSpeedText.equals("walk")) {
      return EHighwayType.LIVING_STREET.getAverageSpeed();
    }
    // "signals" means the speed limit changes according to signal signs, use a
    // default value
    if (maxSpeedText.equals("signals")) {
      return EHighwayType.MOTORWAY.getAverageSpeed();
    }
    // "CH:rural" refers to the maximal speed on rural highways in switzerland
    if (maxSpeedText.equals("CH:rural") || maxSpeedText.equals("DE:rural")) {
      return CH_RURAL_SPEED;
    }
    // "CH:motorway" refers to the maximal speed on motorways in switzerland
    if (maxSpeedText.equals("CH:motorway") || maxSpeedText.equals("DE:motorway")) {
      return CH_MOTORWAY_SPEED;
    }
    // "CH:urban" refers to the maximal speed in urban areas in switzerland
    if (maxSpeedText.equals("CH:urban") || maxSpeedText.equals("DE:urban")) {
      return CH_URBAN_SPEED;
    }
    // "DE:living_street" refers to the maximal speed on living streets in
    // germany
    if (maxSpeedText.equals("DE:living_street")) {
      return DE_LIVING_STREET_SPEED;
    }
    // "DE:walk" refers to the maximal walking speed in germany
    if (maxSpeedText.equals("DE:walk")) {
      return DE_WALK_SPEED;
    }
    // "5p" is a typo referring to "50" (p and 0 are close to each other on the
    // keyboard).
    if (maxSpeedText.equals("5p")) {
      return 50;
    }
    // "tp" is a typo referring to "50" (t and p are close to 5 and 0 on the
    // keyboard).
    if (maxSpeedText.equals("tp")) {
      return 50;
    }
    // "zone" refers to the maximal walking speed in german pedestrian zones
    if (maxSpeedText.equals("DE:zone:30") || maxSpeedText.equals("DE:zone30")) {
      return 30;
    }
    // "zone" refers to the maximal walking speed in german pedestrian zones
    if (maxSpeedText.equals("DE:zone:20")) {
      return 20;
    }
    if (maxSpeedText.equals("=50 maxspeed:conditional=30 @ (08:00-16:00)")) {
      return 50;
    }

    try {
      // 50 mph
      if (maxSpeedText.matches("\\d+ mph")) {
        final int speedAsMph = Integer.parseInt(maxSpeedText.split(" ", 2)[0]);
        return (int) RoutingUtil.mphToKmh(speedAsMph);
      }
      // 50, 60
      if (maxSpeedText.matches("\\d+, \\d+")) {
        final String[] valuesAsText = maxSpeedText.split(", ", 2);
        return Arrays.stream(valuesAsText).mapToInt(Integer::parseInt).min().getAsInt();
      }
      // 50;60
      if (maxSpeedText.matches("\\d+;\\d+")) {
        final String[] valuesAsText = maxSpeedText.split(";", 2);
        return Arrays.stream(valuesAsText).mapToInt(Integer::parseInt).min().getAsInt();
      }
      // 50; 60
      if (maxSpeedText.matches("\\d+; \\d+")) {
        final String[] valuesAsText = maxSpeedText.split("; ", 2);
        return Arrays.stream(valuesAsText).mapToInt(Integer::parseInt).min().getAsInt();
      }
      // 50;60;70
      if (maxSpeedText.matches("\\d+;\\d+;\\d+")) {
        final String[] valuesAsText = maxSpeedText.split(";", 3);
        return Arrays.stream(valuesAsText).mapToInt(Integer::parseInt).min().getAsInt();
      }
      // 50 / 60
      if (maxSpeedText.matches("\\d+ / \\d+")) {
        final String[] valuesAsText = maxSpeedText.split(" / ", 2);
        return Arrays.stream(valuesAsText).mapToInt(Integer::parseInt).min().getAsInt();
      }

      return Integer.parseInt(maxSpeedText);
    } catch (final NumberFormatException e) {
      // Use a default value instead
      LOGGER.error("Can not parse maxspeed value: {}", maxSpeedText);
      return EHighwayType.RESIDENTIAL.getAverageSpeed();
    }
  }

  /**
   * Gets the direction of the given OSM way.
   *
   * @param tagToValue Map connecting the tags of the OSM way to their values
   * @return A value greater than <code>0</code> if the way goes only into the
   *         direction it was declared in the OSM file. A value less than
   *         <code>0</code> if it goes into the opposite direction than declared in
   *         the OSM file. Or <code>0</code> if the way goes into both directions or
   *         the value was not present or could not be parsed.
   */
  public static int parseWayDirection(final Map<String, String> tagToValue) {
    final String onewayText = tagToValue.get(ONEWAY_TAG);
    if (onewayText == null || onewayText.isEmpty() || onewayText.equals("no") || onewayText.equals("false")
        || onewayText.equals("0")) {
      return 0;
    }
    if (onewayText.equals("yes") || onewayText.equals("true") || onewayText.equals("1")) {
      return 1;
    }
    if (onewayText.equals("-1") || onewayText.equals("reverse")) {
      return -1;
    }

    return 0;
  }

  /**
   * Utility class. No implementation.
   */
  private OsmParseUtil() {

  }
}

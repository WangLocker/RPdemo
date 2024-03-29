package com.transitnet.rpdemo.service.route.algotithms.shortestpath.connectionscan;

import com.transitnet.rpdemo.model.Footpath;
import com.transitnet.rpdemo.model.timetable.Connection;

/**
 * POJO for a journey pointer that represents a part of a journey. Can be used
 * for constructing shortest paths by backtracking.<br>
 * <br>
 * A pointer represents a section of a trip together with a final footpath.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 */
public final class JourneyPointer {
  /**
   * The connection used to enter the trip.
   */
  private final Connection mEnterConnection;
  /**
   * The connection used to exit the trip.
   */
  private final Connection mExitConnection;
  /**
   * The footpath used after exiting the trip.
   */
  private final Footpath mFootpath;

  /**
   * Creates a new journey pointer that represents the given path.<br>
   * <br>
   * The connections must belong to the same trip. The enter connection must
   * appear in the trips sequence before the exit connection and the footpath
   * must departure where the exit connection arrives.
   *
   * @param enterConnection The connection used to enter the trip
   * @param exitConnection  The connection used to exit the trip
   * @param footpath        The footpath used after exiting the trip
   */
  public JourneyPointer(final Connection enterConnection, final Connection exitConnection, final Footpath footpath) {
    mEnterConnection = enterConnection;
    mExitConnection = exitConnection;
    mFootpath = footpath;
  }

  /**
   * Gets the connection used to enter the trip.
   *
   * @return The connection used to enter the trip
   */
  public Connection getEnterConnection() {
    return mEnterConnection;
  }

  /**
   * Gets the connection used to exit the trip.
   *
   * @return The connection used to exit the trip
   */
  public Connection getExitConnection() {
    return mExitConnection;
  }

  /**
   * Gets the footpath used after exiting the trip
   *
   * @return The footpath used after exiting the trip
   */
  public Footpath getFootpath() {
    return mFootpath;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("JourneyPointer [enterConnection=");
    builder.append(mEnterConnection);
    builder.append(", exitConnection=");
    builder.append(mExitConnection);
    builder.append(", footpath=");
    builder.append(mFootpath);
    builder.append("]");
    return builder.toString();
  }
}

package com.transitnet.rpdemo.model.transit;

/**
 * Interface for classes that have a time.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 */
public interface IHasTime {
  /**
   * Gets the time of this instance, in seconds since midnight.
   *
   * @return The time of this instance, in seconds since midnight
   */
  int getTime();
}

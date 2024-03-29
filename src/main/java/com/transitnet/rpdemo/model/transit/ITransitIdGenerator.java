package com.transitnet.rpdemo.model.transit;

import java.util.NoSuchElementException;

/**
 * Interface for classes that can generate unique IDs for transit nodes.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 */
public interface ITransitIdGenerator {
  /**
   * Generates and returns an unique ID for transit edges.
   *
   * @return The generated unique ID
   * @throws NoSuchElementException If the generator is out of unique IDs to
   *                                generate
   */
  int generateUniqueEdgeId() throws NoSuchElementException;

  /**
   * Generates and returns an unique ID for transit nodes.
   *
   * @return The generated unique ID
   * @throws NoSuchElementException If the generator is out of unique IDs to
   *                                generate
   */
  int generateUniqueNodeId() throws NoSuchElementException;
}

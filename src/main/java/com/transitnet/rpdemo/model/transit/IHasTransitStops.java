package com.transitnet.rpdemo.model.transit;

import com.transitnet.rpdemo.model.INode;


import java.util.Collection;

/**
 * Interface for classes that accept and offer transit stops.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 * @param <N> Type of the nodes
 */
public interface IHasTransitStops<N extends INode> {
  /**
   * Adds the given stop to this graph.
   *
   * @param stop The stop to add
   * @return <code>True</code> if the stop was added, i.e. not already contained,
   *         <code>false</code> otherwise
   */
  public boolean addStop(TransitStop<N> stop);

  /**
   * Gets all stops of this transit graph.<br>
   * <br>
   * The collection is backed by the graph, changes will be reflected in the
   * graph. Do only change the collection directly if you know the consequences.
   * Else the graph can easily get into a corrupted state.
   *
   * @return All stops of this transit graph
   */
  public Collection<TransitStop<N>> getStops();

  /**
   * Removes the given stop to this graph.
   *
   * @param stop The stop to remove
   * @return <code>True</code> if the stop was removed, i.e. was contained,
   *         <code>false</code> otherwise
   */
  public boolean removeStop(TransitStop<N> stop);
}

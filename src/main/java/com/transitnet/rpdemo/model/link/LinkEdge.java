package com.transitnet.rpdemo.model.link;

import com.transitnet.rpdemo.model.ETransportationMode;
import com.transitnet.rpdemo.model.ICoreEdge;
import com.transitnet.rpdemo.model.ICoreNode;
import com.transitnet.rpdemo.model.IReversedProvider;


import java.util.EnumSet;
import java.util.Set;

/**
 * Link edges link nodes of one graph to nodes of a different graph.<br>
 * <br>
 * Their cost is <code>0</code> and they offer all transportation modes available in
 * a road network. Their ID is always <code>-1</code>.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 * @param <N> The type of nodes
 */
public final class LinkEdge<N extends ICoreNode> implements ICoreEdge<N> {
  /**
   * The serial version UID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The destination of this edge.
   */
  private final N mDestination;
  /**
   * The transportation modes this edge offers.
   */
  private final Set<ETransportationMode> mModes;
  /**
   * An object that provides a reversed flag or <code>null</code> if not present.
   * Can be used to determine if the edge should be interpreted as reversed to
   * implement implicit edge reversal at constant time.
   */
  private IReversedProvider mReversedProvider;
  /**
   * The source of the edge.
   */
  private final N mSource;

  /**
   * Creates a new link edge which connects the given source and destination.
   *
   * @param source      The source node of the edge
   * @param destination The destination node of the edge
   */
  public LinkEdge(final N source, final N destination) {
    mSource = source;
    mDestination = destination;
    mModes = EnumSet.of(ETransportationMode.CAR, ETransportationMode.BIKE, ETransportationMode.FOOT);
  }

  /**
   * Gets the cost of this edge which is always <code>0.0</code>.
   */
  @Override
  public double getCost() {
    return 0.0;
  }

  @Override
  public N getDestination() {
    if (mReversedProvider != null && mReversedProvider.isReversed()) {
      return mSource;
    }
    return mDestination;
  }

  /**
   * Gets the ID of this edge which is always <code>-1</code>.
   */
  @Override
  public int getId() {
    return -1;
  }

  @Override
  public N getSource() {
    if (mReversedProvider != null && mReversedProvider.isReversed()) {
      return mDestination;
    }
    return mSource;
  }

  @Override
  public Set<ETransportationMode> getTransportationModes() {
    return mModes;
  }

  @Override
  public boolean hasTransportationMode(final ETransportationMode mode) {
    return mModes.contains(mode);
  }

  @Override
  public void setReversedProvider(final IReversedProvider provider) {
    mReversedProvider = provider;
  }

}

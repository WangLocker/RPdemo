package com.transitnet.rpdemo.model.road;

import com.transitnet.rpdemo.model.*;
import com.transitnet.rpdemo.service.parse.osm.EHighwayType;
import com.transitnet.rpdemo.util.RoutingUtil;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 实现了{@link IEdge}的道路边，表示一条道路。<br>
 * 有一个{@link EHighwayType}和一个最大速度。它有一个ID，这个ID对于它所属的道路是唯一的。一条道路可以由多个边组成。道路边连接了具有ID和空间的节点。<br>
 */
public final class RoadEdge<N extends ICoreNode> implements ICoreEdge<N>, IRoadEdge {
  /**
   * 这条边在各种交通模态下的成本。以秒为单位，解释为给定高速公路类型的最大允许或平均速度的行驶时间。
   */
  private final Map<ETransportationMode, Double> mCost;
  /**
   * 这条边无视交通模态的的成本。以秒为单位，解释为给定高速公路类型的最大允许或平均速度的行驶时间。
   */
  private double mDefaultCost;
  /**
   * 边的目的地
   */
  private final N mDestination;
  /**
   * 这条边的ID对于它所属的道路是唯一的。一条道路可以由多个边组成。
   */
  private final int mId;
  /**
   * 这条边的最大速度，以<code>km/h</code>为单位
   */
  private final int mMaxSpeed;
  /**
   * 一个提供了一个reversed标志的对象，如果不存在则为<code>null</code>。可以用来确定是否应该将边解释为反向的，以实现常数时间的隐式边反转。
   */
  private IReversedProvider mReversedProvider;
  /**
   * 边的源
   */
  private final N mSource;
  /**
   * 边的道路类型
   */
  private final EHighwayType mType;

  /**
   * 创建一个新的道路边。
   *
   * @param id          一个id，用于表示edge，这个id对于edge所属的way是唯一的。一条way可以由多个edge组成。
   * @param source      源节点
   * @param destination 目的节点
   * @param type        道路类型
   * @param maxSpeed    最大速度，以<code>km/h</code>为单位
   * @param modes       这条边支持的交通模态
   */
  public RoadEdge(final int id, final N source, final N destination, final EHighwayType type, final int maxSpeed,
      final Set<ETransportationMode> modes) {
    mId = id;
    mSource = source;
    mDestination = destination;
    mType = type;
    mMaxSpeed = maxSpeed;

    mCost = new EnumMap<>(ETransportationMode.class);
    for (final ETransportationMode mode : modes) {
      mCost.put(mode, -1.0);
    }

    updateCost();
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof RoadEdge)) {
      return false;
    }
    final RoadEdge<?> other = (RoadEdge<?>) obj;
    if (this.mDestination == null) {
      if (other.mDestination != null) {
        return false;
      }
    } else if (!this.mDestination.equals(other.mDestination)) {
      return false;
    }
    if (this.mId != other.mId) {
      return false;
    }
    if (this.mSource == null) {
      if (other.mSource != null) {
        return false;
      }
    } else if (!this.mSource.equals(other.mSource)) {
      return false;
    }
    if (this.mType != other.mType) {
      return false;
    }
    return true;
  }

  /**
   * The cost of this edge. Measured in seconds, interpreted as travel time with
   * the maximal allowed or average speed for the given highway type.
   */
  @Override
  public double getCost() {
    return mDefaultCost;
  }

  @Override
  public double getCost(final ETransportationMode mode) {
    return mCost.get(mode);
  }

  /*
   * (non-Javadoc)
   * @see
   * de.unifreiburg.informatik.cobweb.routing.model.graph.IEdge#getDestination(
   * )
   */
  @Override
  public N getDestination() {
    if (mReversedProvider != null && mReversedProvider.isReversed()) {
      return mSource;
    }
    return mDestination;
  }

  /**
   * Gets the ID of this edge which is unique to the way it belongs to. A way
   * can consist of several edges.
   */
  @Override
  public int getId() {
    return mId;
  }

  /*
   * (non-Javadoc)
   * @see de.unifreiburg.informatik.cobweb.model.graph.IEdge#getSource()
   */
  @Override
  public N getSource() {
    if (mReversedProvider != null && mReversedProvider.isReversed()) {
      return mDestination;
    }
    return mSource;
  }

  @Override
  public Set<ETransportationMode> getTransportationModes() {
    return mCost.keySet();
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.mDestination == null) ? 0 : this.mDestination.hashCode());
    result = prime * result + this.mId;
    result = prime * result + ((this.mSource == null) ? 0 : this.mSource.hashCode());
    result = prime * result + ((this.mType == null) ? 0 : this.mType.hashCode());
    return result;
  }

  @Override
  public boolean hasTransportationMode(final ETransportationMode mode) {
    return mCost.containsKey(mode);
  }

  /*
   * (non-Javadoc)
   * @see de.unifreiburg.informatik.cobweb.routing.model.graph.road.
   * IReversedConsumer#
   * setReversedProvider(de.unifreiburg.informatik.cobweb.routing.model.graph.
   * road. IReversedProvider)
   */
  @Override
  public void setReversedProvider(final IReversedProvider provider) {
    mReversedProvider = provider;
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("RoadEdge [id=");
    builder.append(mId);
    builder.append(", ");
    builder.append(getSource().getId());
    builder.append(" -(");
    builder.append(mCost);
    builder.append(")-> ");
    builder.append(getDestination().getId());
    builder.append("]");
    return builder.toString();
  }

  /**
   * Recomputes and updates the cost of this edge. Therefore, distances between
   * the given source and destination are computed. Based on that the travel
   * time is computed.<br>
   * <br>
   * The method should be used if the spatial data of the source or destination
   * node changed, as the cost is not updated without calling this method.
   */
  public void updateCost() {
    final double distance = RoutingUtil.distanceEquiRect(mSource, mDestination);
    for (final ETransportationMode mode : mCost.keySet()) {
      mCost.put(mode, computeCost(distance, mode));
    }

    final ETransportationMode fastestMode = Collections.max(mCost.keySet(), new SpeedTransportationModeComparator());
    mDefaultCost = mCost.get(fastestMode);
  }

  /**
   * Computes the cost for this edge. Therefore, the travel time is computed
   * based on the given distance and mode.
   *
   * @param distance The distance to travel in meters
   * @param mode     The transportation mode to use for traveling
   * @return The computed cost for this edge
   */
  private double computeCost(final double distance, final ETransportationMode mode) {
    final double speed = RoutingUtil.getSpeedOfHighway(mType, mMaxSpeed, mode);
    return RoutingUtil.travelTime(distance, speed);
  }

}

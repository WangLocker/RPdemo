package com.transitnet.rpdemo.model.road;

import com.transitnet.rpdemo.model.ETransportationMode;

/**
 * 道路边的接口
 */
public interface IRoadEdge {
  /**
   * 该条边在给定交通模态下的成本。以秒为单位，解释为给定高速公路类型的最大允许或平均速度的行驶时间。
   */
  public double getCost(ETransportationMode mode);
}

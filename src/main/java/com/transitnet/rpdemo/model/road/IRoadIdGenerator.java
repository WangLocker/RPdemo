package com.transitnet.rpdemo.model.road;

import java.util.NoSuchElementException;

/**
 * 为道路生成唯一ID的接口
 */
public interface IRoadIdGenerator {
  /**
   * 为node生成唯一ID
   */
  int generateUniqueNodeId() throws NoSuchElementException;

  /**
   * 为way生成唯一ID
   */
  int generateUniqueWayId() throws NoSuchElementException;
}

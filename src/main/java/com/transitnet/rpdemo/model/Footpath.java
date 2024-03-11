package com.transitnet.rpdemo.model;

/**
 * 连接站点的步道，权值为用时
 */
public final class Footpath {
    /**
     * 步道的到达站点的id
     */
    private final int mArrStopId;
    /**
     * 步道的出发站点的id
     */
    private final int mDepStopId;
    /**
     * 用时
     */
    private int mDuration;

    /**
     * 构造函数，创建一段新的步道
     */
    public Footpath(final int depStopId, final int arrStopId, final int duration) {
        mDepStopId = depStopId;
        mArrStopId = arrStopId;
        mDuration = duration;
    }

    /**
     * 获得步道的到达站点的id
     */
    public int getArrStopId() {
        return mArrStopId;
    }

    /**
     * 获得步道的出发站点的id
     */
    public int getDepStopId() {
        return mDepStopId;
    }

    /**
     * 获得步道用时，单位为s
     */
    public int getDuration() {
        return mDuration;
    }

    /**
     * 设置步道用时
     */
    public void setDuration(final int duration) {
        mDuration = duration;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Footpath [");
        builder.append(mDepStopId);
        builder.append(" -> ");
        builder.append(mArrStopId);
        builder.append(", duration=");
        builder.append(mDuration);
        builder.append("]");
        return builder.toString();
    }
}
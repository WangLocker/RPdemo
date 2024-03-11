package com.transitnet.rpdemo.model.timetable;

import com.transitnet.rpdemo.model.ICoreNode;

/**
 * 公共交通网络中的一个站点，包含其位置和id
 */
public final class Stop implements ICoreNode {
    /**
     * 唯一id
     */
    private final int mId;
    /**
     * 纬度
     */
    private float mLatitude;
    /**
     * 经度
     */
    private float mLongitude;

    /**
     * 构造函数，新站点
     */
    public Stop(final int id, final float latitude, final float longitude) {
        mId = id;
        mLatitude = latitude;
        mLongitude = longitude;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Stop)) {
            return false;
        }
        final Stop other = (Stop) obj;
        if (this.mId != other.mId) {
            return false;
        }
        return true;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public float getLatitude() {
        return mLatitude;
    }

    @Override
    public float getLongitude() {
        return mLongitude;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.mId;
        return result;
    }

    @Override
    public void setLatitude(final float latitude) {
        mLatitude = latitude;
    }

    @Override
    public void setLongitude(final float longitude) {
        mLongitude = longitude;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Stop [id=");
        builder.append(mId);
        builder.append(", latitude=");
        builder.append(mLatitude);
        builder.append(", longitude=");
        builder.append(mLongitude);
        builder.append("]");
        return builder.toString();
    }
}

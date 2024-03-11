package com.transitnet.rpdemo.pojo;

public final class IdMapping {
    /**
     * 系统内部的id
     */
    private final int mInternalId;

    /**
     * 这个映射是为node还是为way的映射
     */
    private final boolean mIsNode;
    /**
     * 这个映射的OSM ID。
     */
    private final long mOsmId;

    /**
     * 创建一个新的OSM ID到内部ID的映射。
     */
    public IdMapping(final long osmId, final int internalId, final boolean isNode) {
        mOsmId = osmId;
        mInternalId = internalId;
        mIsNode = isNode;
    }

    /**
     * 获取这个映射的内部ID。
     */
    public int getInternalId() {
        return mInternalId;
    }

    /**
     * 获取这个映射的OSM ID。
     */
    public long getOsmId() {
        return mOsmId;
    }

    /**
     * 这个映射是为node还是为way的映射。
     */
    public boolean isNode() {
        return mIsNode;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("IdMapping [isNode=");
        builder.append(mIsNode);
        builder.append(", osmId=");
        builder.append(mOsmId);
        builder.append(", internalId=");
        builder.append(mInternalId);
        builder.append("]");
        return builder.toString();
    }
}

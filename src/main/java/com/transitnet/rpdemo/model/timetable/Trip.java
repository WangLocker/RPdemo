package com.transitnet.rpdemo.model.timetable;

import org.eclipse.collections.impl.list.mutable.FastList;

import java.io.Serializable;
import java.util.List;

public final class Trip implements Serializable {
    /**
     * 旅程的唯一id
     */
    private final int mId;
    /**
     * 这段旅程代表的connection的序列
     */
    private final List<Connection> mSequence;

    /**
     * 创建一个新的旅程
     */
    public Trip(final int id) {
        mId = id;
        mSequence = FastList.newList();
    }

    /**
     * 向旅程末尾添加给定的connection
     */
    public void addConnectionToSequence(final Connection connection) {
        mSequence.add(connection);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Trip)) {
            return false;
        }
        final Trip other = (Trip) obj;
        if (this.mId != other.mId) {
            return false;
        }
        return true;
    }

    /**
     * 用于获取连接序列中指定索引位置的连接对象
     */
    public Connection getConnectionAtSequenceIndex(final int sequenceIndex) throws IndexOutOfBoundsException {
        if (sequenceIndex < 0 || sequenceIndex >= mSequence.size()) {
            throw new IndexOutOfBoundsException();
        }
        return mSequence.get(sequenceIndex);
    }


    public int getId() {
        return mId;
    }

    /**
     * 获取由这个旅程表示的连接序列
     */
    public List<Connection> getSequence() {
        return mSequence;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.mId;
        return result;
    }
}
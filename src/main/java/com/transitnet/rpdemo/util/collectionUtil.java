package com.transitnet.rpdemo.util;

import java.util.Collection;

public final class collectionUtil {
    /**
     * 增加给定集合的内部容量，直到集合具有所需的容量为止，通过追加<code>null</code>值来实现。
     */
    public static <E> void increaseCapacity(final Collection<E> collection, final int capacity) {
        for (int i = collection.size(); i < capacity; i++) {
            collection.add(null);
        }
    }

    private collectionUtil() {

    }
}

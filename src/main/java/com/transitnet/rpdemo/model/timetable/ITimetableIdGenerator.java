package com.transitnet.rpdemo.model.timetable;

import java.util.NoSuchElementException;

public interface ITimetableIdGenerator {
    /**
     为站点生成唯一ID
     */
    int generateUniqueStopId() throws NoSuchElementException;

    /**
     为旅程生成唯一ID
     */
    int generateUniqueTripId() throws NoSuchElementException;
}

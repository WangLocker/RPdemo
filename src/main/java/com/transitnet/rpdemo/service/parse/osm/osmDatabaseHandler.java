package com.transitnet.rpdemo.service.parse.osm;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.model.iface.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

@Service
public final class osmDatabaseHandler implements IOsmFileHandler {

    @Autowired
    com.transitnet.rpdemo.service.parse.osm.databaseOprator databaseOprator;
    /**
     * 缓冲区大小
     */
    private static final int BUFFER_SIZE = 100_000;
    /**
     * 日志记录器
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(osmDatabaseHandler.class);
    /**
     * 节点的解析是否已经完成
     */
    private boolean mAreNodesFinished;
    /**
     * 当前索引，指向下一个元素可以插入的索引。因此，它总是比最后插入的元素的索引大一。因此，它表示缓冲区的当前占用大小。
     */
    private int mBufferIndex;
    /**
     * 用于缓冲将要推送到数据库的实体的缓冲区。使用缓冲区是为了避免在单个连接中推送每个元素到数据库。
     */
    private final OsmEntity[] mEntityBuffer;


    /**
     * 构造函数
     */
    public osmDatabaseHandler(){
        mEntityBuffer = new OsmEntity[BUFFER_SIZE];
    }


    @Override
    public void complete() throws IOException {
        // Submit buffer
        offerBuffer();
    }


    @Override
    public void handle(final OsmBounds bounds) throws IOException {
        // Ignore
    }


    @Override
    public void handle(final OsmNode node) throws IOException {
        handleEntity(node);
    }


    @Override
    public void handle(final OsmRelation relation) throws IOException {
        handleEntity(relation);
    }


    @Override
    public void handle(final OsmWay way) throws IOException {
        if (!mAreNodesFinished) {
            // Flush the buffer to ensure database has all nodes
            mAreNodesFinished = true;
            offerBuffer();
        }
        handleEntity(way);
    }

    /**
     * Handles the given OSM entity. Therefore, it collects the entity to a
     * buffer. If the buffer is full it will be offered to the database in order
     * to push the collected data. Depending on the size of the buffer and the
     * type of database this method may take a while when the buffer is full.
     *
     * @param entity The entity to handle
     */
    private void handleEntity(final OsmEntity entity) {
        // If buffer is full, offer it
        if (mBufferIndex >= mEntityBuffer.length) {
            offerBuffer();
        }

        // Collect the entity, index has changed due to offer
        mEntityBuffer[mBufferIndex] = entity;

        // Increase the index
        mBufferIndex++;
    }

    /**
     * Offers the buffer to the database in order to push the data to the
     * database. Afterwards, the buffer index is reset to implicitly clear the
     * buffer.
     */
    private void offerBuffer() {
        // Offer all items up to the current index
        final int size = mBufferIndex;
        if (size == 0) {
            return;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Offering buffer of size: {}", size);
        }
        databaseOprator.offerOsmEntities(Arrays.stream(mEntityBuffer, 0, size), size);

        // Reset index since buffer is empty again
        mBufferIndex = 0;
    }

}

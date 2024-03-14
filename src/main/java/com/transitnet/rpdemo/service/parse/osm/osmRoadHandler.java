package com.transitnet.rpdemo.service.parse.osm;

import com.transitnet.rpdemo.model.*;
import com.transitnet.rpdemo.pojo.SpatialNodeData;
import com.transitnet.rpdemo.pojo.IdMapping;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Service
public final class osmRoadHandler<N extends INode & IHasId & ISpatial, E extends IEdge<N> & IHasId,
        G extends IGraph<N, E> & IGetNodeById<N>> implements IOsmFileHandler{
    /**
     * 缓冲区大小
     */
    private static final int BUFFER_SIZE = 100_000;
    /**
     * 日志记录器
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(osmRoadHandler.class);
    /**
     * 用于缓冲将要提交到数据库的节点ID映射的缓冲区。使用缓冲区是为了避免在单个连接中提交每个映射的数据到数据库。
     */
    private IdMapping[] mBufferedNodeMappings;
    /**
     * 用于缓冲将要从数据库请求空间节点数据的OSM节点ID的缓冲区。使用缓冲区是为了避免在单个连接中为每个节点请求数据。
     */
    private long[] mBufferedSpatialRequests;
    /**
     * 用于缓冲将要提交到数据库的道路ID映射的缓冲区。使用缓冲区是为了避免在单个连接中提交每个映射的数据到数据库。
     */
    private IdMapping[] mBufferedWayMappings;
    /**
     * 当前索引，指向下一个元素可以插入的索引。因此，它总是比最后插入的元素的索引大一。因此，它表示缓冲区的当前占用大小。
     */
    private int mNodeMappingBufferIndex;
    /**
     * 当前索引，指向下一个元素可以插入的索引。因此，它总是比最后插入的元素的索引大一。因此，它表示缓冲区的当前占用大小。
     */
    private int mSpatialRequestBufferIndex;
    /**
     * 当前索引，指向下一个元素可以插入的索引。因此，它总是比最后插入的元素的索引大一。因此，它表示缓冲区的当前占用大小。
     */
    private int mWayMappingBufferIndex;
    /**
     * 用于过滤道路的OSM过滤器。
     */
    @Autowired
    private osmFilter mFilter;
    /**
     * 用于请求空间节点数据的数据库。
     */
    @Autowired
    private databaseOprator mDatabase;
    /**
     * 用于构建将要插入图中的边和节点的构建器。
     */
    private IosmRoadBuilder<N,E> mBuilder;

    /**
     * 用于插入解析的节点和边的图。
     */
    private G mGraph;


    /**
     * Creates a new OSM road handler which operates on the given graph using the
     * given configuration.<br>
     * <br>
     * The filter is used to filter OSM road ways. The builder will be used to
     * construct the nodes and edges that are to be inserted into the graph. The
     * database offers spatial node data for nodes.
     *
     */
    public osmRoadHandler() {
        mBufferedNodeMappings = new IdMapping[BUFFER_SIZE];
        mBufferedWayMappings = new IdMapping[BUFFER_SIZE];
        mBufferedSpatialRequests = new long[BUFFER_SIZE];
    }

    public void setBuilderAndGraph(IosmRoadBuilder<N,E> builder, G graph){
        mBuilder = builder;
        mGraph = graph;
    }

    /*
     * (non-Javadoc)
     * @see de.topobyte.osm4j.core.access.OsmHandler#complete()
     */
    @Override
    public void complete(){
        // Submit buffers, note that the order is important
        submitBufferedNodeMappings();
        submitBufferedWayMappings();
        submitBufferedSpatialRequests();


        mBuilder.complete();
    }


    @Override
    public void handle(final OsmBounds bounds) throws IOException {
    }

    @Override
    public void handle(final OsmNode node) throws IOException {
    }

    @Override
    public void handle(final OsmRelation relation) throws IOException {
    }

    @Override
    public void handle(final OsmWay way) throws IOException {
        // Return if filter does not accept
        if (!mFilter.filter(way)) {
            return;
        }

        // Determine way direction
        final Map<String, String> tagToValue = OsmModelUtil.getTagsAsMap(way);
        final int wayDirection = OsmParseUtil.parseWayDirection(tagToValue);

        Integer internalWayId = null;

        // Iterate all nodes
        long sourceIdOsm = -1;
        boolean isFirstIteration = true;
        for (int i = 0; i < way.getNumberOfNodes(); i++) {
            final long destinationIdOsm = way.getNodeId(i);
            // Attempt to add the current node, spatial data is unknown at first
            final Node destinationNode = new Node(destinationIdOsm, 0.0, 0.0);
            final N node = mBuilder.buildNode(destinationNode);
            final boolean wasAdded = mGraph.addNode(node);
            // Request spatial data of the node and register the ID mapping
            if (wasAdded) {
                // It is important that the mappings are in the database when
                // requesting spatial data.
                queueNodeIdMapping(destinationIdOsm, node.getId());
                try {
                    queueSpatialNodeRequest(destinationIdOsm);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            // Update and yield the first iteration
            if (isFirstIteration) {
                sourceIdOsm = destinationIdOsm;
                isFirstIteration = false;
                continue;
            }

            // Create an edge
            if (wayDirection >= 0) {
                final E edge = mBuilder.buildEdge(way, sourceIdOsm, destinationIdOsm);
                internalWayId = edge.getId();
                mGraph.addEdge(edge);
            }
            if (wayDirection <= 0) {
                final E edge = mBuilder.buildEdge(way, destinationIdOsm, sourceIdOsm);
                internalWayId = edge.getId();
                mGraph.addEdge(edge);
            }

            // Update for the next iteration
            sourceIdOsm = destinationIdOsm;
        }

        // Queue way ID mapping
        if (internalWayId != null) {
            queueWayIdMapping(way.getId(), internalWayId);
        }
    }


    /**
     * Inserts the given spatial node data into the graph. That is, it finds the
     * node and updates its spatial data according to the given data.<br>
     * <br>
     * The node represented by the data must exist in the graph.
     *
     * @param data The data to insert
     */
    private void insertSpatialData(final SpatialNodeData data) {
        final Optional<N> possibleNode = mGraph.getNodeById(data.getId());
        // Node must be present since we added it before requesting
        if (!possibleNode.isPresent()) {
            throw new AssertionError();
        }
        // Set data to node
        final N node = possibleNode.get();
        node.setLatitude(data.getLatitude());
        node.setLongitude(data.getLongitude());
    }

    /**
     * Queues a node ID mapping which is to be pushed to the database. The data is
     * buffered and the buffer is submitted using
     * {@link #submitBufferedNodeMappings()}.
     *
     * @param osmId      The unique OSM ID of the mapping to queue
     * @param internalId The internal ID of the mapping to queue
     */
    private void queueNodeIdMapping(final long osmId, final int internalId) {
        // If buffer is full, submit it
        if (mNodeMappingBufferIndex >= mBufferedNodeMappings.length) {
            submitBufferedNodeMappings();
        }

        // Collect the mapping, index has changed due to submit
        mBufferedNodeMappings[mNodeMappingBufferIndex] = new IdMapping(osmId, internalId, true);

        // Increase index
        mNodeMappingBufferIndex++;
    }

    /**
     * Queues a spatial node data request for the given node. The request is
     * buffered and the buffer is submitted using
     * {@link #submitBufferedSpatialRequests()}.
     *
     * @param nodeIdOsm The unique OSM ID of the node to queue a request for
     */
    private void queueSpatialNodeRequest(final long nodeIdOsm) throws SQLException {
        // If buffer is full, submit it
        if (mSpatialRequestBufferIndex >= mBufferedSpatialRequests.length) {
            submitBufferedSpatialRequests();
        }

        // Collect the node, index has changed due to submit
        mBufferedSpatialRequests[mSpatialRequestBufferIndex] = nodeIdOsm;

        // Increase index
        mSpatialRequestBufferIndex++;
    }

    /**
     * Queues a way ID mapping which is to be pushed to the database. The data is
     * buffered and the buffer is submitted using
     * {@link #submitBufferedWayMappings()}.
     *
     * @param osmId      The unique OSM ID of the mapping to queue
     * @param internalId The internal ID of the mapping to queue
     */
    private void queueWayIdMapping(final long osmId, final int internalId) {
        // If buffer is full, submit it
        if (mWayMappingBufferIndex >= mBufferedWayMappings.length) {
            submitBufferedWayMappings();
        }

        // Collect the mapping, index has changed due to submit
        mBufferedWayMappings[mWayMappingBufferIndex] = new IdMapping(osmId, internalId, false);

        // Increase index
        mWayMappingBufferIndex++;
    }

    /**
     * Submits the buffered node ID mappings. The mappings are send to the given
     * database.<br>
     * <br>
     * Afterwards, the buffer index is reset to implicitly clear the buffer.
     * Ideally, this method is only used when the buffer is full.
     */
    private void submitBufferedNodeMappings() {
        // Send all buffered mappings up to the current index
        final int size = mNodeMappingBufferIndex;
        if (size == 0) {
            return;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Submitting node ID mappings of size: {}", size);
        }
        mDatabase.offerIdMappings(Arrays.stream(mBufferedNodeMappings, 0, size), size);

        // Reset index since buffer is empty again
        mNodeMappingBufferIndex = 0;
    }

    /**
     * Submits the buffered requests. The requests are send to the given database
     * and the spatial node data are inserted into the graph using
     * {@link #insertSpatialData(SpatialNodeData)}.<br>
     * <br>
     * Afterwards, the buffer index is reset to implicitly clear the buffer.
     * Ideally, this method is only used when the buffer is full.
     */
    private void submitBufferedSpatialRequests() {
        // Send all buffered requests up to the current index
        final int size = mSpatialRequestBufferIndex;
        if (size == 0) {
            return;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Submitting buffered requests of size: {}", size);
        }
        final Collection<SpatialNodeData> nodeData =
                mDatabase.getSpatialNodeData(Arrays.stream(mBufferedSpatialRequests, 0, size), size);
        if (nodeData.size() < size) {
            LOGGER.error("Database did not deliver spatial data for all {} nodes, lost: {}", size, size - nodeData.size());
        }
        nodeData.forEach(this::insertSpatialData);

        // Reset index since buffer is empty again
        mSpatialRequestBufferIndex = 0;
    }

    /**
     * Submits the buffered way ID mappings. The mappings are send to the given
     * database.<br>
     * <br>
     * Afterwards, the buffer index is reset to implicitly clear the buffer.
     * Ideally, this method is only used when the buffer is full.
     */
    private void submitBufferedWayMappings() {
        // Send all buffered mappings up to the current index
        final int size = mWayMappingBufferIndex;
        if (size == 0) {
            return;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Submitting way ID mappings of size: {}", size);
        }
        mDatabase.offerIdMappings(Arrays.stream(mBufferedWayMappings, 0, size), size);

        // Reset index since buffer is empty again
        mWayMappingBufferIndex = 0;
    }
}

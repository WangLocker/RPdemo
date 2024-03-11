package com.transitnet.rpdemo.service.parse.osm;

import com.transitnet.rpdemo.model.UniqueIdGenerator;
import com.transitnet.rpdemo.util.E;
import com.transitnet.rpdemo.util.IdMap;
import com.transitnet.rpdemo.util.N;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

public final class roadGraphOperator {

    /**
     * 一个映射，将节点ID连接到相应的节点。
     */
    private MutableIntObjectMap<N> mIdToNode;
    /**
     * 图当前是否被反转。
     */
    private boolean mIsReversed;
    /**
     * 用于节点的唯一ID生成器。
     */
    private UniqueIdGenerator mNodeIdGenerator;
    /**
     * 一个将节点连接到其传入边的映射。
     */
    private  Map<N, Set<E>> mNodeToIncomingEdges;
    /**
     * 一个将节点连接到其传出边的映射。
     */
    private Map<N, Set<E>> mNodeToOutgoingEdges;
    /**
     * 用于路径的唯一ID生成器。
     */
    private UniqueIdGenerator mWayIdGenerator;

    /**
     * 创建一个新的初始为空的道路图。
     */
    public roadGraphOperator() {
        mIdToNode = IntObjectMaps.mutable.empty();
        mNodeIdGenerator = new UniqueIdGenerator();
        mWayIdGenerator = new UniqueIdGenerator();

        // Assume node IDs are close to each other and have no, or only few, gaps.
        mNodeToIncomingEdges = new IdMap<>();
        mNodeToOutgoingEdges = new IdMap<>();
    }

    public boolean addEdge(final E edge) {
        edge.setReversedProvider(this);
        return super.addEdge(edge);
    }

    public boolean addNode(final N node) {
        final int id = node.getId();
        if (mIdToNode.containsKey(id)) {
            return false;
        }
        mIdToNode.put(id, node);
        return true;
    }

    public boolean containsNodeWithId(final int id) {
        return mIdToNode.containsKey(id);
    }

    public int generateUniqueNodeId() throws NoSuchElementException {
        return mNodeIdGenerator.generateUniqueId();
    }

    public int generateUniqueWayId() throws NoSuchElementException {
        return mWayIdGenerator.generateUniqueId();
    }

    public Stream<E> getEdges() {
        if (mNodeToOutgoingEdges instanceof IdMap) {
            final IdMap<N, Set<E>> asIdMap = (IdMap<N, Set<E>>) mNodeToOutgoingEdges;
            // Fall back to streamValues() since IdMap does not support values()
            return asIdMap.streamValues().flatMap(Collection::stream);
        }
        return super.getEdges();
    }

    public Optional<N> getNodeById(final int id) {
        return Optional.ofNullable(mIdToNode.get(id));
    }

    /**
     * Gets a collection of all nodes that the graph contains.<br>
     * <br>
     * The collection is backed by the graph, changes will be reflected in the
     * graph. Do only change the collection directly if you know the consequences.
     * Else the graph can easily get into a corrupted state. In many situations it
     * is best to use the given methods like {@link #addNode(INode)} instead.
     */
    public Collection<N> getNodes() {
        return mIdToNode.values();
    }

    public boolean isReversed() {
        return mIsReversed;
    }

    public boolean removeNode(final N node) {
        final int id = node.getId();
        if (!mIdToNode.containsKey(id)) {
            return false;
        }

        // Remove all incoming and outgoing edges
        getIncomingEdges(node).forEach(this::removeEdge);
        getOutgoingEdges(node).forEach(this::removeEdge);

        mIdToNode.remove(id);
        return true;
    }

    /**
     * Reverses the graph. That is, all directed edges switch source with
     * destination.<br>
     * <br>
     * The implementation runs in constant time, edge reversal is only made
     * implicit.
     */
    public void reverse() {
        mIsReversed = !mIsReversed;
    }

    protected Set<E> constructEdgeSetWith(final E edge) {
        // Assume that edge sets only contain a very limited amount of edges.
        return new HybridArrayHashSet<>(edge);
    }

    protected Map<N, Set<E>> getNodeToIncomingEdges() {
        if (mIsReversed) {
            return mNodeToOutgoingEdges;
        }
        return mNodeToIncomingEdges;
    }

    protected Map<N, Set<E>> getNodeToOutgoingEdges() {
        if (mIsReversed) {
            return mNodeToIncomingEdges;
        }
        return mNodeToOutgoingEdges;
    }

}

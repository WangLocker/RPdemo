package com.transitnet.rpdemo.service.parse.osm;

import com.transitnet.rpdemo.model.*;
import com.transitnet.rpdemo.model.road.IRoadIdGenerator;
import com.transitnet.rpdemo.model.road.RoadEdge;
import com.transitnet.rpdemo.model.road.RoadNode;
import com.transitnet.rpdemo.service.parse.ParseException;
import com.transitnet.rpdemo.util.RoutingUtil;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import org.eclipse.collections.api.map.primitive.MutableLongIntMap;
import org.eclipse.collections.impl.factory.primitive.LongIntMaps;

import java.util.Map;

/**
 * Implementation of an {@link IosmRoadBuilder} which builds instances of
 * {@link RoadNode}s and {@link RoadEdge}s.<br>
 * <br>
 * Edges are build incomplete at first and are updated once {@link #complete()}
 * is called.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 * @param <G> The type of the graph which must be able to get nodes by their IDs
 */
public final class osmRoadBuilder<G extends IGraph<ICoreNode, ICoreEdge<ICoreNode>> & IGetNodeById<ICoreNode>>
        implements IosmRoadBuilder<ICoreNode, ICoreEdge<ICoreNode>>{
    /**
     * The graph to operate on.
     */
    private G mGraph;
    /**
     * A generator which provides unique IDs for nodes and ways.
     */
    private final IRoadIdGenerator mIdGenerator;
    /**
     * A map connecting OSM node IDs to the IDs used by the graph.
     */
    private MutableLongIntMap mOsmToNodeId;
    /**
     * A map connecting OSM way IDs to the IDs used by the graph.
     */
    private MutableLongIntMap mOsmToWayId;

    /**
     * Creates a new OSM road builder which operates on the given graph.
     *
     * @param graph       The graph to operate on
     * @param idGenerator The generator to use for generating unique IDs for nodes
     *                    and ways
     */
    public osmRoadBuilder(final G graph, final IRoadIdGenerator idGenerator) {
        mGraph = graph;
        mIdGenerator = idGenerator;
        mOsmToNodeId = LongIntMaps.mutable.empty();
        mOsmToWayId = LongIntMaps.mutable.empty();
    }

    /**
     * Builds an edge which connects the source and destination nodes with the
     * given unique IDs.<br>
     * <br>
     * The edge is build incomplete at first and are updated once
     * {@link #complete()} is called.
     */
    @Override
    public ICoreEdge<ICoreNode> buildEdge(final OsmWay way, final long sourceIdOsm, final long destinationIdOsm)
            throws ParseException {
        final int sourceId = mOsmToNodeId.getIfAbsentPut(sourceIdOsm, () -> {
            throw new ParseException();
        });
        final int destinationId = mOsmToNodeId.getIfAbsentPut(destinationIdOsm, () -> {
            throw new ParseException();
        });
        final ICoreNode source = mGraph.getNodeById(sourceId).orElseThrow(() -> new ParseException());
        final ICoreNode destination = mGraph.getNodeById(destinationId).orElseThrow(() -> new ParseException());

        // Get information about the highway type
        final Map<String, String> tagToValue = OsmModelUtil.getTagsAsMap(way);
        final EHighwayType type = OsmParseUtil.parseHighwayType(tagToValue);
        final int maxSpeed = OsmParseUtil.parseMaxSpeed(tagToValue);

        // Lookup if the way is already known or generate a new ID
        final int wayId = mOsmToWayId.getIfAbsentPut(way.getId(), () -> mIdGenerator.generateUniqueWayId());
        return new RoadEdge<>(wayId, source, destination, type, maxSpeed,
                RoutingUtil.getTransportationModesOfHighway(type));
    }

    /*
     * (non-Javadoc)
     * @see de.unifreiburg.informatik.cobweb.routing.parsing.osm.IOsmRoadBuilder#
     * buildNode(de. topobyte. osm4j.core.model.iface.OsmNode)
     */
    @Override
    public RoadNode buildNode(final OsmNode node) {
        // Lookup if the node is already known or generate a new ID
        final int nodeId = mOsmToNodeId.getIfAbsentPut(node.getId(), () -> mIdGenerator.generateUniqueNodeId());
        return new RoadNode(nodeId, (float) node.getLatitude(), (float) node.getLongitude());
    }

    /**
     * Callback to be used once construction has been finished.<br>
     * <br>
     * Will update all constructed edges since they are build incomplete at first.
     */
    @Override
    public void complete() {
        // Spatial data of nodes are now known, update all edge costs
        mGraph.getEdges().filter(RoadEdge.class::isInstance).map(RoadEdge.class::cast).forEach(RoadEdge::updateCost);
    }

}

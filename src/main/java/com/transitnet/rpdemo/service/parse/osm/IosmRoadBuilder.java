package com.transitnet.rpdemo.service.parse.osm;

import com.transitnet.rpdemo.model.IEdge;
import com.transitnet.rpdemo.model.IHasId;
import com.transitnet.rpdemo.model.INode;
import com.transitnet.rpdemo.model.ISpatial;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;

/**
 * Interface for objects that can construct roads out of OSM nodes and ways.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 * @param <N> Type of the node
 * @param <E> Type of the edge
 */
public interface IosmRoadBuilder<N extends INode & IHasId & ISpatial, E extends IEdge<N> & IHasId> {
  /**
   * Builds an edge which connects the source and destination nodes with the
   * given unique IDs.
   *
   * @param way              The OSM way this edge is part of
   * @param sourceIdOsm      The unique OSM ID of the source node this edge
   *                         connects
   * @param destinationIdOsm The unique OSM ID of the destination node this edge
   *                         connects
   * @return The constructed edge
   */
  E buildEdge(OsmWay way, long sourceIdOsm, long destinationIdOsm);

  /**
   * Builds a node which represents the given OSM node. The node must share the
   * unique ID of the OSM node.
   *
   * @param node The OSM node this node represents
   * @return The constructed node
   */
  N buildNode(OsmNode node);

  /**
   * Callback to be used once construction has been finished.
   */
  void complete();
}

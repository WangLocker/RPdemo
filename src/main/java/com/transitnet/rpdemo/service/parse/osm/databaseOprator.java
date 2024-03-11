package com.transitnet.rpdemo.service.parse.osm;

import com.transitnet.rpdemo.dao.*;
import com.transitnet.rpdemo.entity.osmNodeEntity;
import com.transitnet.rpdemo.entity.osmNodeTagEntity;
import com.transitnet.rpdemo.entity.osmWayTagEntity;
import com.transitnet.rpdemo.pojo.IdMapping;
import com.transitnet.rpdemo.pojo.SpatialNodeData;
import com.transitnet.rpdemo.util.DatabaseUtil;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmEntity;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@Service
public class databaseOprator {
    @Autowired
    osmNodeDao osmNodeDao;
    @Autowired
    osmWayTagDao osmWayTagDao;
    @Autowired
    osmNodeMappingDao osmNodeMappingDao;
    @Autowired
    osmWayMappingDao osmWayMappingDao;
    @Autowired
    osmNodeTagDao osmNodeTagDao;

    private static final Logger LOGGER = LoggerFactory.getLogger(databaseOprator.class);

    public void offerOsmEntities(final Stream<OsmEntity> entities, final int size) {
        LOGGER.info("Offering {} entities to the database", size);
        entities.forEach(entity -> {
            if (entity instanceof OsmNode) {
                queueOsmNode((OsmNode)entity);
            } else if (entity instanceof OsmWay) {
                queueOsmWay((OsmWay)entity);
            }
        });
    }

    public void offerIdMappings(final Stream<IdMapping> mappings, final int size) {
        LOGGER.debug("Offering {} ID mappings to the database", size);
        mappings.forEach(mapping -> {
            if(mapping.isNode()){
                osmNodeMappingDao.insertNodeMapping(mapping.getInternalId(), mapping.getOsmId());
            } else{
                osmWayMappingDao.insertWayMapping(mapping.getInternalId(), mapping.getOsmId());
            }
        });
    }

    private void queueOsmNode(final OsmNode node){
        // Retrieve information
        final long id = node.getId();
        final float latitude = (float) node.getLatitude();
        final float longitude = (float) node.getLongitude();
        final Map<String, String> tagToValue = OsmModelUtil.getTagsAsMap(node);
        final String name = tagToValue.get(OsmParseUtil.NAME_TAG);
        final String highway = tagToValue.get(OsmParseUtil.HIGHWAY_TAG);

        // Insert node data
        osmNodeDao.insertNode(id, latitude, longitude);
        // Insert tag data
        osmNodeTagDao.insertNodeTag(id, name, highway);
    }

    private void queueOsmWay(final OsmWay way){
        final long wayId = way.getId();
        final Map<String, String> tagToValue = OsmModelUtil.getTagsAsMap(way);
        final String name = tagToValue.get(OsmParseUtil.NAME_TAG);
        final String highway = tagToValue.get(OsmParseUtil.HIGHWAY_TAG);
        Integer maxSpeed = OsmParseUtil.parseMaxSpeed(tagToValue);
        if (maxSpeed == -1) {
            maxSpeed = null;
        }

        // Insert tag data
        osmWayTagDao.insertWayTag(wayId, name, highway, maxSpeed);
    }

    public Collection<SpatialNodeData> getSpatialNodeData(final LongStream nodeIds, final int size){
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Getting spatial data for {} nodes", size);
        }
        final List<SpatialNodeData> nodeData = new ArrayList<>(size);
        List<Long> nodeList = nodeIds.boxed().collect(Collectors.toList());
        ResultSet result=osmNodeDao.getSpatialNodes(nodeList);

        try {
            while (result.next()) {
                final long osmId = result.getLong(1);
                final int internalId = result.getInt(2);
                final float latitude = result.getFloat(3);
                final float longitude = result.getFloat(4);
                nodeData.add(new SpatialNodeData(internalId, osmId, latitude, longitude));
            }
        } catch (SQLException e) {
            LOGGER.error("Error while fetching spatial node data", e);
        }
        return nodeData;
    }
}

package com.transitnet.rpdemo.service.parse.osm;

import com.transitnet.rpdemo.dao.*;
import com.transitnet.rpdemo.entity.osmNodeEntity;
import com.transitnet.rpdemo.entity.osmNodeTagEntity;
import com.transitnet.rpdemo.entity.osmWayTagEntity;
import com.transitnet.rpdemo.pojo.IdMapping;
import com.transitnet.rpdemo.pojo.SpatialNodeData;
import de.topobyte.osm4j.core.model.iface.OsmEntity;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
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
        AtomicInteger i= new AtomicInteger();
        List<osmNodeEntity> l1=new ArrayList<>();
        List<osmNodeTagEntity> l2=new ArrayList<>();
        List<osmWayTagEntity> l3=new ArrayList<>();
        entities.forEach(entity -> {
            if (entity instanceof OsmNode) {
                List<Object> temp=buildOsmNode((OsmNode)entity);
                l1.add((osmNodeEntity)temp.get(0));
                l2.add((osmNodeTagEntity)temp.get(1));
            } else if (entity instanceof OsmWay) {
                buildOsmWay((OsmWay)entity);
                l3.add(buildOsmWay((OsmWay)entity));
            }
        });
        osmNodeDao.saveAll(l1);
        osmNodeTagDao.saveAll(l2);
        osmWayTagDao.saveAll(l3);
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

    private List<Object> buildOsmNode(final OsmNode node){
        // Retrieve information
        final long id = node.getId();
        final float latitude = (float) node.getLatitude();
        final float longitude = (float) node.getLongitude();
        final Map<String, String> tagToValue = OsmModelUtil.getTagsAsMap(node);
        final String name = tagToValue.get(OsmParseUtil.NAME_TAG);
        final String highway = tagToValue.get(OsmParseUtil.HIGHWAY_TAG);

        // Insert node data
        osmNodeEntity object1 = new osmNodeEntity();
        object1.setId(id);
        object1.setLatitude(latitude);
        object1.setLongitude(longitude);
        // Insert tag data
        osmNodeTagEntity object2 = new osmNodeTagEntity();
        object2.setId(id);
        object2.setName(name);
        object2.setHighway(highway);
        return Arrays.asList(object1, object2);
    }

    private osmWayTagEntity buildOsmWay(final OsmWay way){
        final long wayId = way.getId();
        final Map<String, String> tagToValue = OsmModelUtil.getTagsAsMap(way);
        final String name = tagToValue.get(OsmParseUtil.NAME_TAG);
        final String highway = tagToValue.get(OsmParseUtil.HIGHWAY_TAG);
        Integer maxSpeed = OsmParseUtil.parseMaxSpeed(tagToValue);
        if (maxSpeed == -1) {
            maxSpeed = null;
        }

        // Insert tag data
        osmWayTagEntity object = new osmWayTagEntity();
        object.setId(wayId);
        object.setName(name);
        object.setHighway(highway);
        object.setMaxspeed(maxSpeed);
        return object;
    }

    public Collection<SpatialNodeData> getSpatialNodeData(final LongStream nodeIds, final int size){
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Getting spatial data for {} nodes", size);
        }
        List<SpatialNodeData> nodeData;
        List<Long> nodeList = nodeIds.boxed().collect(Collectors.toList());
        nodeData=osmNodeDao.getSpatialNodes(nodeList);

        return nodeData;
    }
}

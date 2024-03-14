package com.transitnet.rpdemo.dao;

import com.transitnet.rpdemo.entity.osmNodeEntity;
import com.transitnet.rpdemo.pojo.SpatialNodeData;
import org.hibernate.query.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.List;

@Repository
public interface osmNodeDao extends JpaRepository<osmNodeEntity, Long> {

    @Modifying
    @Query(value = "REPLACE INTO osm_nodes (id, latitude, longitude) VALUES (:id, :latitude, :longitude)", nativeQuery = true)
    void insertNode(@Param("id") long id, @Param("latitude") float latitude, @Param("longitude") float longitude);
    @Query("SELECT new com.transitnet.rpdemo.pojo.SpatialNodeData(mappings.internalId,mappings.osmId, nodes.latitude, nodes.longitude)" +
            "FROM osmNodeEntity nodes, osmNodeMappingEntity mappings " +
            "WHERE nodes.id = mappings.osmId AND nodes.id IN :nodeIds")
    List<SpatialNodeData> getSpatialNodes(List<Long> nodeIds);



}

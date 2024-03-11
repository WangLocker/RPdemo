package com.transitnet.rpdemo.dao;

import com.transitnet.rpdemo.entity.osmNodeEntity;
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
    @Transactional
    @Query(value = "REPLACE INTO osm_nodes (id, latitude, longitude) VALUES (:id, :latitude, :longitude)", nativeQuery = true)
    void insertNode(@Param("id") long id, @Param("latitude") float latitude, @Param("longitude") float longitude);

    @Query("SELECT mappings.osmId, mappings.internalId, nodes.latitude, nodes.longitude " +
            "FROM osmNodeEntity nodes, osmNodeMappingEntity mappings " +
            "WHERE nodes.id = mappings.osmId AND nodes.id IN :nodeIds")
    ResultSet getSpatialNodes(List<Long> nodeIds);
}

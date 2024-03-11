package com.transitnet.rpdemo.dao;

import com.transitnet.rpdemo.entity.osmNodeMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface osmNodeMappingDao extends JpaRepository<osmNodeMappingEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = "REPLACE INTO osm_node_mappings (internal_id, osm_id) VALUES (:internalId, :osmId)", nativeQuery = true)
    void insertNodeMapping(@Param("internalId") int internalId, @Param("osmId") long osmId);
}


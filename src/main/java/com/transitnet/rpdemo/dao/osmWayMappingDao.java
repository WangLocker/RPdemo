package com.transitnet.rpdemo.dao;

import com.transitnet.rpdemo.entity.osmWayMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface osmWayMappingDao extends JpaRepository<osmWayMappingEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = "REPLACE INTO osm_way_mappings (internal_id, osm_id) VALUES (:internalId, :osmId)", nativeQuery = true)
    void insertWayMapping(@Param("internalId") int internalId, @Param("osmId") long osmId);
}


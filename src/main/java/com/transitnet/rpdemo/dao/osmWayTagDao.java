package com.transitnet.rpdemo.dao;

import com.transitnet.rpdemo.entity.osmNodeTagEntity;
import com.transitnet.rpdemo.entity.osmWayTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public interface osmWayTagDao extends JpaRepository<osmWayTagEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = "REPLACE INTO osm_way_tags (id, name, highway, maxspeed) VALUES (:id, :name, :highway, :maxspeed)", nativeQuery = true)
    void insertWayTag(@Param("id") long id, @Param("name") String name, @Param("highway") String highway, @Param("maxspeed") int maxspeed);
}
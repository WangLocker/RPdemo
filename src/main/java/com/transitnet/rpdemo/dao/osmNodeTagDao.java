package com.transitnet.rpdemo.dao;

import com.transitnet.rpdemo.entity.osmNodeEntity;
import com.transitnet.rpdemo.entity.osmNodeTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public interface osmNodeTagDao extends JpaRepository<osmNodeTagEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = "REPLACE INTO osm_node_tags (id, name, highway) VALUES (:id, :name, :highway)", nativeQuery = true)
    void insertNodeTag(@Param("id") long id, @Param("name") String name, @Param("highway") String highway);
}

package com.transitnet.rpdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "osm_node_mappings")
public class osmNodeMappingEntity {
    @Id
    @Column(name = "internal_id")
    private Integer internalId;

    @Column(name = "osm_id")
    private Long osmId;

    @Id
    public Integer getInternalId() {
        return internalId;
    }
    public void setInternalId(Integer internalId) {
        this.internalId = internalId;
    }

    public Long getOsmId() {
        return osmId;
    }
    public void setOsmId(Long osmId) {
        this.osmId = osmId;
    }

}

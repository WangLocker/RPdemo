package com.transitnet.rpdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "osm_way_tags")
public class osmWayTagEntity {
    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "name", length = 45)
    private String name;

    @Column(name = "highway", length = 45)
    private String highway;

    @Column(name = "maxspeed")
    private Integer maxspeed;


    public void setId(Long id) {
        this.id = id;
    }

    @Id
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHighway() {
        return highway;
    }

    public void setHighway(String highway) {
        this.highway = highway;
    }

    public Integer getMaxspeed() {
        return maxspeed;
    }

    public void setMaxspeed(Integer maxspeed) {
        this.maxspeed = maxspeed;
    }
}

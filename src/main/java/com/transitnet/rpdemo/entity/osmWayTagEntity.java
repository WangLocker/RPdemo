package com.transitnet.rpdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

public class osmWayTagEntity {
    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "name", length = 45)
    private String name;

    @Column(name = "highway", length = 45)
    private String highway;

    @Column(name = "maxspeed")
    private int maxspeed;


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

    public int getMaxspeed() {
        return maxspeed;
    }

    public void setMaxspeed(int maxspeed) {
        this.maxspeed = maxspeed;
    }
}
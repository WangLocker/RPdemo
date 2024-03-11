package com.transitnet.rpdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "osm_node_tags")
public class osmNodeTagEntity {
    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "name", length = 45)
    private String name;

    @Column(name = "highway", length = 45)
    private String highway;

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
}

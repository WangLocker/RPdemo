package com.transitnet.rpdemo.service.parse.osm;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public final class osmHandlerForwarder implements OsmHandler {
    /**
     * 下面这个迭代器是用来存储所有作为转发目标的OsmHandler的
     */
    private final Iterable<? extends OsmHandler> mAllHandler;

    /**
     * 构造函数
     */
    public osmHandlerForwarder(final Iterable<? extends OsmHandler> allHandler) {
        mAllHandler = allHandler;
    }

    /**
     * 对所有给出的处理器转发complete方法
     */
    @Override
    public void complete() throws IOException {
        for (final OsmHandler handler : mAllHandler) {
            handler.complete();
        }
    }

    /**
     * 对所有给出的处理器转发handle方法，处理bound类型的OSM实体
     */
    @Override
    public void handle(final OsmBounds bounds) throws IOException {
        for (final OsmHandler handler : mAllHandler) {
            handler.handle(bounds);
        }
    }

    /**
     * 对所有给出的处理器转发handle方法，处理node类型的OSM实体
     */
    @Override
    public void handle(final OsmNode node) throws IOException {
        for (final OsmHandler handler : mAllHandler) {
            handler.handle(node);
        }
    }

    /**
     * 对所有给出的处理器转发handle方法，处理relation类型的OSM实体
     */
    @Override
    public void handle(final OsmRelation relation) throws IOException {
        for (final OsmHandler handler : mAllHandler) {
            handler.handle(relation);
        }
    }

    /**
     * 对所有给出的处理器转发handle方法，处理way类型的OSM实体
     */
    @Override
    public void handle(final OsmWay way) throws IOException {
        for (final OsmHandler handler : mAllHandler) {
            handler.handle(way);
        }
    }

}

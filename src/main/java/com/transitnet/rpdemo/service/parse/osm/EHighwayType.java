package com.transitnet.rpdemo.service.parse.osm;

import java.util.HashMap;
import java.util.Map;

/**
 * 道路类型
 */
public enum EHighwayType {
    /**
     * 自行车道
     */
    CYCLEWAY("cycleway", 14),
    /**
     * 街道
     */
    LIVING_STREET("living_street", 7),
    /**
     * 快速高速公路
     */
    MOTORWAY("motorway", 120),
    /**
     * 高速公路连接
     */
    MOTORWAY_LINK("motorway_link", 50),
    /**
     * 主要道路
     */
    PRIMARY("primary", 100),
    /**
     * 主要道路连接
     */
    PRIMARY_LINK("primary_link", 50),
    /**
     * 住宅道路
     */
    RESIDENTIAL("residential", 50),
    /**
     * 未知道路
     */
    ROAD("road", 20),
    /**
     * 次要道路
     */
    SECONDARY("secondary", 80),
    /**
     * 次要道路连接
     */
    SECONDARY_LINK("secondary_link", 50),
    /**
     * 服务道路
     */
    SERVICE("service", 7),
    /**
     * 三级道路
     */
    TERTIARY("tertiary", 70),
    /**
     * 主干道
     */
    TRUNK("trunk", 110),
    /**
     * 主干道连接
     */
    TRUNK_LINK("trunk_link", 50),
    /**
     * 未分类道路
     */
    UNCLASSIFIED("unclassified", 40),
    /**
     * 未铺设道路
     */
    UNSURFACED("unsurfaced", 30);

    /**
     * Map 将道路标签名映射到其类型
     */
    private static final Map<String, EHighwayType> NAME_TO_TYPE = EHighwayType.constructLookupTable();

    /**
     * 从名称获取道路类型
     */
    public static EHighwayType fromName(final String name) {
        return NAME_TO_TYPE.get(name);
    }

    /**
     * 构造查找表
     */
    private static Map<String, EHighwayType> constructLookupTable() {
        final HashMap<String, EHighwayType> nameToType = new HashMap<>();

        for (final EHighwayType type : EHighwayType.values()) {
            nameToType.put(type.getName(), type);
        }

        return nameToType;
    }

    /**
     * 平均速度
     */
    private final int mAverageSpeed;
    /**
     * 名称
     */
    private final String mName;

    /**
     * 构造函数
     */
    private EHighwayType(final String textValue, final int averageSpeed) {
        mName = textValue;
        mAverageSpeed = averageSpeed;
    }

    /**
     * 获取平均速度
     */
    public int getAverageSpeed() {
        return mAverageSpeed;
    }

    /**
     * 获取名称
     */
    public String getName() {
        return mName;
    }
}
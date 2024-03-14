package com.transitnet.rpdemo.service.route;

import com.transitnet.rpdemo.model.ICoreEdge;
import com.transitnet.rpdemo.model.ICoreNode;
import com.transitnet.rpdemo.model.IGetNodeById;
import com.transitnet.rpdemo.model.IGraph;
import com.transitnet.rpdemo.model.road.RoadGraph;
import com.transitnet.rpdemo.model.timetable.Timetable;
import com.transitnet.rpdemo.service.parse.ParseException;
import com.transitnet.rpdemo.service.parse.osm.IosmRoadBuilder;
import com.transitnet.rpdemo.service.parse.osm.databaseOprator;
import com.transitnet.rpdemo.service.parse.osm.osmRoadBuilder;
import com.transitnet.rpdemo.service.parse.osm.osmRoadHandler;
import com.transitnet.rpdemo.service.route.algotithms.metrics.AsTheCrowFliesMetric;
import com.transitnet.rpdemo.service.route.algotithms.nearestneighbor.CoverTree;
import com.transitnet.rpdemo.service.route.algotithms.nearestneighbor.INearestNeighborComputation;
import com.transitnet.rpdemo.service.route.algotithms.shortestpath.ShortestPathComputationFactory;
import com.transitnet.rpdemo.service.route.algotithms.shortestpath.hybridmodel.IAccessNodeComputation;
import com.transitnet.rpdemo.service.route.algotithms.shortestpath.hybridmodel.RoadToKNearestTransitAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;


@Service
public class routingModelService {
    private static final Logger LOGGER = LoggerFactory.getLogger(routingModelService.class);
    @Value("${routingmodel.accessNodesMaximum}")
    private int AccessNodesMaximum;
    @Value("${routingmodel.abortTravelTimeToAccessNodes}")
    private int AbortTravelTimeToAccessNodes;
    @Value("${routingmodel.amountOfLandmarks}")
    private int AmountOfLandmarks;
    @Value("${routingmodel.transferDelay}")
    private int TransferDelay;
    @Value("${routingmodel.footpathReachability}")
    private int FootpathReachability;
    @Autowired
    private databaseOprator mDatabase;
    @Autowired
    private osmRoadHandler osmRoadHandler;
    /**
     * 最近邻road nodes计算器
     */
    private INearestNeighborComputation<ICoreNode> mNearestRoadNodeComputation;
    /**
     * 路网图
     */
    private RoadGraph<ICoreNode, ICoreEdge<ICoreNode>> mRoadGraph;
    /**
     * 时刻表模型
     */
    private Timetable mTimetable;

    public routingModelService() {
        LOGGER.info("Initializing model");
        mTimetable = new Timetable();
        mRoadGraph = new RoadGraph<>();
    }

    public void configOsmHandler(){
        final IosmRoadBuilder<ICoreNode, ICoreEdge<ICoreNode>> roadBuilder = new osmRoadBuilder<>(mRoadGraph, mRoadGraph);
        osmRoadHandler.setBuilderAndGraph(roadBuilder, mRoadGraph);
    }

    /**
     * Creates a factory used for creating shortest path computation algorithm.
     * The exact factory depends on the routing model mode.
     *
     * @return The constructed factory
     */
    public ShortestPathComputationFactory createShortestPathComputationFactory() {
        final Instant preCompTimeStart = Instant.now();
        final ShortestPathComputationFactory factory;
        final IAccessNodeComputation<ICoreNode, ICoreNode> accessNodeComputation =
                new RoadToKNearestTransitAccess(mTimetable, AccessNodesMaximum);
        factory = new ShortestPathComputationFactory(mRoadGraph, mTimetable, accessNodeComputation,
                        mNearestRoadNodeComputation,AbortTravelTimeToAccessNodes,AmountOfLandmarks);

        factory.initialize();

        final Instant preCompTimeEnd = Instant.now();
        LOGGER.info("Precomputation took: {}", Duration.between(preCompTimeStart, preCompTimeEnd));

        return factory;
    }

    /**
     * Finishes the preparation of the model. This may serialize the model.
     *
     * @throws ParseException If an exception occurred while parsing data like
     *                        configuration files or if an exception at
     *                        serialization occurred
     */
    public void finishModel() throws ParseException {
        final int currentGraphSize;
        currentGraphSize = mRoadGraph.size();
        //增加cache缓存逻辑
    }


    /**
     * Gets the algorithm to use for nearest road node computation.
     *
     * @return The algorithm to use for nearest road node computation
     */
    public INearestNeighborComputation<ICoreNode> getNearestRoadNodeComputation() {
        return mNearestRoadNodeComputation;
    }

    /**
     * Gets a node provider that is able to get nodes by their ID.
     *
     * @return A node provider
     */
    public IGetNodeById<ICoreNode> getNodeProvider() {
        return mRoadGraph;
    }

    /**
     * Gets the query graph used by this model. That is the graph that provides
     * the nodes to query on.
     *
     * @return The query graph used by this model
     */
    public IGraph<ICoreNode, ICoreEdge<ICoreNode>> getQueryGraph() {
        return mRoadGraph;
    }

    /**
     * Gets information about the size of the routing model.
     *
     * @return A human readable information of the model size
     */
    public String getSizeInformation() {
        return toString();
    }


    public void prepareModelAfterData() {
        initializeNearestRoadNodeComputation();
        // Road graph is implicitly linked by access node computation which is
        // done on-the-fly
        // Correct the footpath model of the timetable
        mTimetable.correctFootpaths(TransferDelay, FootpathReachability);
    }


    /**
     * Initializes the nearest road node computation.
     */
    private void initializeNearestRoadNodeComputation() {
        LOGGER.info("Initializing nearest road node computation");
        final Instant nearestNeighborsStartTime = Instant.now();

        final CoverTree<ICoreNode> nearestRoadNodeComputation = new CoverTree<>(new AsTheCrowFliesMetric<>());
        for (final ICoreNode node : mRoadGraph.getNodes()) {
            nearestRoadNodeComputation.insert(node);
        }

        mNearestRoadNodeComputation = nearestRoadNodeComputation;

        final Instant nearestNeighborsEndTime = Instant.now();
        LOGGER.info("Nearest road node took: {}", Duration.between(nearestNeighborsStartTime, nearestNeighborsEndTime));
    }
}

package com.transitnet.rpdemo.model.timetable;

import com.transitnet.rpdemo.model.Footpath;
import com.transitnet.rpdemo.model.UniqueIdGenerator;
import com.transitnet.rpdemo.util.RoutingUtil;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class Timetable implements ITimetableIdGenerator, Serializable {
    /**
     * 注册日志记录器
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Timetable.class);

    /**
     * 时刻表模型中包含的footpath总数
     */
    private int mAmountOfFootpaths;
    /**
     * 按照出发时间升序排列的所有connection
     */
    private final List<Connection> mConnections;
    /**
     * 一个数据结构，用于建立反映站点间可达性的映射
     */
    private final MutableIntObjectMap<MutableIntSet> mFootpathReachability;
    /**
     * 目前时刻表中最大的站点ID
     */
    private int mGreatestStopId;
    /**
     * 目前时刻表中最大的旅程ID
     */
    private int mGreatestTripId;
    /**
     * 一个数据结构，用于将ID映射到其对应的站点
     */
    private final MutableIntObjectMap<Stop> mIdToStop;
    /**
     * 一个数据结构，用于将ID映射到其对应的旅程
     */
    private final MutableIntObjectMap<Trip> mIdToTrip;
    /**
     * 站点id生成器
     */
    private final UniqueIdGenerator mStopIdGenerator;
    /**
     * 将ID映射到所有的向外的footpath集合
     */
    private final MutableIntObjectMap<Collection<Footpath>> mStopIdToOutgoingFootpaths;
    /**
     * 旅程id生成器
     */
    private final UniqueIdGenerator mTripIdGenerator;

    /**
     * 创建一个新的空的时刻表
     */
    public Timetable() {
        mStopIdGenerator = new UniqueIdGenerator();
        mTripIdGenerator = new UniqueIdGenerator();
        mConnections = new ArrayList<>();
        mIdToStop = IntObjectMaps.mutable.empty();
        mIdToTrip = IntObjectMaps.mutable.empty();
        mStopIdToOutgoingFootpaths = IntObjectMaps.mutable.empty();
        mFootpathReachability = IntObjectMaps.mutable.empty();
    }

    /**
     * 将给定的connection加入到timetable中
     * 该方法不适合频繁调用，因为连接列表是一个有序列表，添加一个新的连接会导致列表重新排序
     *
     * @param connections The connections to add
     */
    public void addConnections(final Collection<Connection> connections) {
        final boolean hasChanged = mConnections.addAll(connections);
        if (hasChanged) {
            Collections.sort(mConnections);
        }
    }

    /**
     * 将给定的footpath加入到timetable中
     */
    public void addFootpath(final Footpath footpath) {
        mStopIdToOutgoingFootpaths.getIfAbsentPut(footpath.getDepStopId(), FastList::new).add(footpath);
        mFootpathReachability.getIfAbsentPut(footpath.getDepStopId(), IntSets.mutable.empty()).add(footpath.getArrStopId());
        mAmountOfFootpaths++;
    }

    /**
     * 将给定的stop加入到timetable中
     */
    public void addStop(final Stop stop) {
        mIdToStop.put(stop.getId(), stop);
    }

    /**
     * 将给定的trip加入到timetable中
     */
    public void addTrip(final Trip trip) {
        mIdToTrip.put(trip.getId(), trip);
    }

    /**
     * 通过添加自循环步道和补充缺失的步道保证步道正确
     */
    public void correctFootpaths(final int transferDelay, final int footpathReachability) {
        LOGGER.debug("Correcting footpaths");

        // 确保已经存在的步道满足三角不等式
        LOGGER.debug("Ensuring triangle inequality on existing footpaths");
        final AtomicInteger incorrectFootpathCounter = new AtomicInteger();
        mStopIdToOutgoingFootpaths.stream().flatMap(Collection::stream)
                .filter(footpath -> footpath.getDuration() < transferDelay).forEach(footpath -> {
                    footpath.setDuration(transferDelay);
                    incorrectFootpathCounter.incrementAndGet();
                });
        LOGGER.debug("Corrected durations of {} footpaths", incorrectFootpathCounter.get());

        // Add missing self-loops
        LOGGER.debug("Computing missing self-loops");

        final Collection<Footpath> selfLoopsToAdd = FastList.newList();
        mIdToStop.keysView().forEach(fromId -> {
            final MutableIntSet reachableStopIds = mFootpathReachability.get(fromId);
            if (reachableStopIds == null || !reachableStopIds.contains(fromId)) {
                // Self-loop is missing
                selfLoopsToAdd.add(new Footpath(fromId, fromId, transferDelay));
            }
        });
        selfLoopsToAdd.forEach(this::addFootpath);
        LOGGER.debug("Adding {} self-loops", selfLoopsToAdd.size());

        // Connect close stops
        LOGGER.debug("Connecting close stops");
        final Collection<Footpath> closeFootpathsToAdd = FastList.newList();
        mIdToStop.values().forEach(fromStop -> {
            final int fromStopId = fromStop.getId();
            final MutableIntSet reachableStopIds = mFootpathReachability.get(fromStopId);
            mIdToStop.values().stream().forEach(toStop -> {
                // Ignore already reachable stops
                if (reachableStopIds != null && reachableStopIds.contains(toStop.getId())) {
                    return;
                }

                // Do not consider stop as target if not close enough
                final double distance = RoutingUtil.distanceEquiRect(fromStop, toStop);
                if (distance > footpathReachability) {
                    return;
                }

                // Construct footpath
                final double speed = RoutingUtil.getWalkingSpeed();
                // Ensure the duration is at least the transfer time to ensure triangle
                // inequality when taking self-loops
                final int duration = (int) Math.max(transferDelay, RoutingUtil.travelTime(distance, speed));
                closeFootpathsToAdd.add(new Footpath(fromStopId, toStop.getId(), duration));
            });
        });
        closeFootpathsToAdd.forEach(this::addFootpath);
        LOGGER.debug("Adding {} footpaths to close stops", closeFootpathsToAdd.size());

        // Compute transitive closure
        LOGGER.debug("Computing transitive closure");
        final Collection<Footpath> transitiveClosureToAdd = FastList.newList();
        // Breadth-first-search per stop
        mIdToStop.keysView().forEach(fromStopId -> {
            // Find all reachable stops
            final Queue<Integer> stopsToRelax = new ArrayDeque<>();
            stopsToRelax.add(fromStopId);
            final MutableIntSet deepReachable = IntSets.mutable.empty();
            deepReachable.add(fromStopId);

            while (!stopsToRelax.isEmpty()) {
                final int currentStop = stopsToRelax.poll();
                final MutableIntSet directReachable = mFootpathReachability.get(currentStop);
                if (directReachable == null) {
                    continue;
                }
                directReachable.forEach(directTarget -> {
                    // Target was not visited already
                    if (!deepReachable.contains(directTarget)) {
                        stopsToRelax.add(directTarget);
                        deepReachable.add(directTarget);
                    }
                });
            }

            // Compute the difference between direct and deep reachable, those are the
            // edges to add for the transitive closure
            if (mFootpathReachability != null) {
                deepReachable.removeAll(mFootpathReachability.get(fromStopId));
            }
            final Stop fromStop = mIdToStop.get(fromStopId);
            deepReachable.forEach(toStopId -> {
                final Stop toStop = mIdToStop.get(toStopId);
                // Construct footpath
                final double distance = RoutingUtil.distanceEquiRect(fromStop, toStop);
                final double speed = RoutingUtil.getWalkingSpeed();
                // Ensure the duration is at least the transfer time to ensure triangle
                // inequality when taking self-loops
                final int duration = (int) Math.max(transferDelay, RoutingUtil.travelTime(distance, speed));
                transitiveClosureToAdd.add(new Footpath(fromStopId, toStopId, duration));
            });
        });
        transitiveClosureToAdd.forEach(this::addFootpath);
        LOGGER.debug("Adding {} footpaths for transitive closure", transitiveClosureToAdd.size());
    }

    @Override
    public int generateUniqueStopId() throws NoSuchElementException {
        final int id = mStopIdGenerator.generateUniqueId();
        if (id > mGreatestStopId) {
            mGreatestStopId = id;
        }
        return id;
    }

    @Override
    public int generateUniqueTripId() throws NoSuchElementException {
        final int id = mTripIdGenerator.generateUniqueId();
        if (id > mGreatestTripId) {
            mGreatestTripId = id;
        }
        return id;
    }

    /**
     * Creates an iterator which returns all connections of this table, starting
     * with the first connection departing after, or exactly at, the given
     * time.<br>
     * <br>
     * Note that this also includes connections departing at the day after. The
     * iterator ends after all connections have been traversed.
     *
     * @param time The time to get connections since, in seconds since midnight.
     * @return An iterator over all connections, starting with the first
     *         connection departing not before the given time
     */
//    public Iterator<Connection> getConnectionsStartingSince(final int time) {
//        final Connection searchNeedle = new Connection(-1, -1, -1, -1, time, time);
//        final int indexOfNext = -1 * Collections.binarySearch(mConnections, searchNeedle) - 1;
//
//        // If all connections are before the given time
//        if (indexOfNext == mConnections.size()) {
//            // Use a regular iterator starting from the first element
//            return mConnections.iterator();
//        }
//
//        // Use a ranged overflow iterator based on random access
//        return new RangedOverflowListIterator<>(mConnections, indexOfNext);
//    }

    /**
     * Gets the greatest ID currently in use for a stop in this table.
     *
     * @return The greatest ID currently in use for a stop in this table
     */
    public int getGreatestStopId() {
        return mGreatestStopId;
    }

    /**
     * Gets the greatest ID currently in use for a trip in this table.
     *
     * @return The greatest ID currently in use for a trip in this table
     */
    public int getGreatestTripId() {
        return mGreatestTripId;
    }

    /**
     * Gets a stream over all footpaths going out of the given stop.
     *
     * @param stopId The unique ID fo the stop to get footpaths from
     * @return A stream over all footpaths going out of the given stop
     */
    public Stream<Footpath> getOutgoingFootpaths(final int stopId) {
        return mStopIdToOutgoingFootpaths.get(stopId).stream();
    }

    /**
     * Gets a human readable string that contains size information of the table,
     * i.e. the amount of stops, trips and connections.
     *
     * @return A human readable string containing size information
     */
    public String getSizeInformation() {
        return toString();
    }

    /**
     * Gets the stop with the given ID.
     *
     * @param id The unique ID of the stop to get
     * @return The stop with the given ID
     */
    public Stop getStop(final int id) {
        return mIdToStop.get(id);
    }

    /**
     * Gets a collection of all stops contained in the table.
     *
     * @return A collection of all stops contained in the table
     */
    public Collection<Stop> getStops() {
        return mIdToStop.values();
    }

    /**
     * Gets the trip with the given ID.
     *
     * @param id The unique ID of the trip to get
     * @return The trip with the given ID
     */
    public Trip getTrip(final int id) {
        return mIdToTrip.get(id);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringJoiner sj = new StringJoiner(", ", getClass().getSimpleName() + "[", "]");
        sj.add("stops=" + mIdToStop.size());
        sj.add("trips=" + mIdToTrip.size());
        sj.add("connections=" + mConnections.size());
        sj.add("footpaths=" + mAmountOfFootpaths);
        return sj.toString();
    }
}
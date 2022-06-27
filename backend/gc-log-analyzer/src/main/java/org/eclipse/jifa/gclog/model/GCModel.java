/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.jifa.gclog.model;

import org.eclipse.jifa.common.listener.ProgressListener;
import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.gclog.model.TimeLineChartView.TimeLineChartEvent;
import org.eclipse.jifa.gclog.util.DoubleData;
import org.eclipse.jifa.gclog.util.LongData;
import org.eclipse.jifa.gclog.vo.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.vo.PageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.eclipse.jifa.gclog.model.GCEvent.*;
import static org.eclipse.jifa.gclog.model.GCEventType.*;
import static org.eclipse.jifa.gclog.vo.GCCollectorType.*;
import static org.eclipse.jifa.gclog.vo.HeapGeneration.*;

/**
 * GCModel contains all direct information from log and analysed data for query
 */
public abstract class GCModel {
    protected static final Logger LOGGER = LoggerFactory.getLogger(GCModel.class);

    private List<GCEvent> allEvents = new ArrayList<>(); // store all event, order by their appearance in log
    private List<GCEvent> gcEvents = new ArrayList<>(); // store parent events only, order by start tune
    private List<GCEvent> gcCollectionEvents = new ArrayList<>(); // store events that contain collection info
    // time from beginning of program
    private double startTime = UNKNOWN_DOUBLE;
    private double endTime = UNKNOWN_DOUBLE;

    private int parallelThread = UNKNOWN_INT;
    private int concurrentThread = UNKNOWN_INT;

    // in ms. referenceTimestamp + uptime of events is the true timestamp of events.
    // notice that uptime may not begin from 0
    private double referenceTimestamp = UNKNOWN_DOUBLE;
    //shared basic info among different collectors
    private VmOptions vmOptions;

    //data prepared for api
    private GCCollectorType collectorType;
    private GCLogStyle logStyle;
    private GCLogMetadata metadata;
    private List<GCPhaseInfo> gcPhaseInfos;
    private List<String> gcEventsDetailCache;

    private List<ProblemAndSuggestion> problemAndSuggestion = new ArrayList<>();

    public static final String NA = "N/A";

    public static final double MS2S = 1e3;
    public static final double KB2MB = 1 << 10;
    public static final double START_TIME_ZERO_THRESHOLD = 60000;
    public static final double EPS = 1e-6;

    public GCModel() {
    }

    public GCModel(GCCollectorType collectorType) {
        this.collectorType = collectorType;
    }

    public void setCollectorType(GCCollectorType collectorType) {
        this.collectorType = collectorType;
    }

    public GCCollectorType getCollectorType() {
        return collectorType;
    }

    public GCLogStyle getLogStyle() {
        return logStyle;
    }

    public void setLogStyle(GCLogStyle logStyle) {
        this.logStyle = logStyle;
    }

    public List<GCEvent> getGcCollectionEvents() {
        return gcCollectionEvents;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public void setStartTime(double startTime) {
        if (startTime < START_TIME_ZERO_THRESHOLD) {
            startTime = 0;
        }
        this.startTime = startTime;
    }

    protected boolean isGenerational() {
        return true;
    }

    protected boolean isPauseless() {
        return false;
    }

    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    public double getDuration() {
        return getEndTime() - getStartTime();
    }

    public boolean isEmpty() {
        return gcEvents.isEmpty();
    }

    public List<GCEvent> getGcEvents() {
        return gcEvents;
    }

    public void setGcEvents(List<GCEvent> gcEvents) {
        this.gcEvents = gcEvents;
    }

    public GCEvent createAndGetEvent() {
        GCEvent event = new GCEvent(gcEvents.size());
        gcEvents.add(event);
        return event;
    }

    public <T extends TimedEvent> void iterateEventsWithinTimeRange(List<T> eventList, TimeRange range, Consumer<T> consumer) {
        int indexLow = binarySearchEventIndex(eventList, range.getStart(), true);
        int indexHigh = binarySearchEventIndex(eventList, range.getEnd(), false);

        for (int i = indexLow; i < indexHigh; i++) {
            consumer.accept(eventList.get(i));
        }
    }


    // Return index of the first event after time if searchLow, first event after time if !searchLow  .
    // eventList must be ordered by startTime.
    private int binarySearchEventIndex(List<? extends TimedEvent> eventList, double time, boolean searchLow) {
        if (searchLow && time <= getStartTime()) {
            return 0;
        } else if (!searchLow && time >= getEndTime()) {
            return eventList.size();
        }

        TimedEvent eventForSearching = new TimedEvent();
        eventForSearching.setStartTime(time);
        int result = Collections.binarySearch(eventList, eventForSearching, Comparator.comparingDouble(TimedEvent::getStartTime));
        if (result < 0) {
            return -(result + 1);
        } else {
            if (searchLow) {
                while (result >= 0 && eventList.get(result).getStartTime() >= time) {
                    result--;
                }
                return result + 1;
            } else {
                while (result < eventList.size() && eventList.get(result).getStartTime() <= time) {
                    result++;
                }
                return result;
            }
        }
    }

    private TimeRange makeValidTimeRange(TimeRange range) {
        if (range == null) {
            return new TimeRange(getStartTime(), getEndTime());
        }
        double start = Math.max(range.getStart(), getStartTime());
        double end = Math.min(range.getEnd(), getEndTime());
        return new TimeRange(start, end);
    }

    public ObjectStatistics getObjectStatistics(TimeRange range) {
        range = makeValidTimeRange(range);
        LongData allocation = new LongData();
        LongData promotion = new LongData();
        iterateEventsWithinTimeRange(gcCollectionEvents, range, event -> {
            allocation.add(event.getAllocation());
            promotion.add(event.getPromotion());
        });
        return new ObjectStatistics(
                allocation.getSum() != UNKNOWN_DOUBLE ? allocation.getSum() / range.length() : UNKNOWN_DOUBLE,
                promotion.getSum() != UNKNOWN_DOUBLE ? promotion.getSum() / range.length() : UNKNOWN_DOUBLE,
                (long) promotion.average(), promotion.getMax()
        );
    }

    // decide start and end time using events
    public void autoDecideStartEndTime() {
        gcEvents.sort(Comparator.comparingDouble(GCEvent::getStartTime));
        if (this.getEndTime() != UNKNOWN_DOUBLE) {
            return;
        }
        if (gcEvents == null || gcEvents.size() == 0) {
            return;
        }
        setStartTime(gcEvents.get(0).getEndTime());
        GCEvent event = gcEvents.get(gcEvents.size() - 1);
        double endTime = event.getEndTime();
        if (event.hasPhases()) {
            endTime = event.getPhases().get(event.getPhases().size() - 1).getEndTime();
        }
        setEndTime(endTime);
    }

    public int getRecommendMaxHeapSize() {
        throw new UnsupportedOperationException();
    }

    public void putEvent(GCEvent event) {
        gcEvents.add(event);
        allEvents.add(event);
    }

    public void addPhase(GCEvent parent, GCEvent phase) {
        allEvents.add(phase);
        parent.addPhase(phase);
    }

    public List<GCPhaseInfo> getGcPhaseInfo() {
        return gcPhaseInfos;
    }

    private static final List<GCCollectorType> SUPPORTED_COLLECTORS = List.of(G1, CMS, SERIAL, PARALLEL, UNKNOWN, ZGC);

    private boolean isGCCollectorSupported(GCCollectorType collectorType) {
        return SUPPORTED_COLLECTORS.contains(collectorType);
    }

    public void calculateDerivedInfo(ProgressListener progressListener) {
        if (!isGCCollectorSupported(collectorType)) {
            throw new UnsupportedOperationException("Collector not supported.");
        }
        // must be done before other steps
        filterInvalidEvents();
        autoDecideStartEndTime();
        decideAndFixEventInfo();

        // let subclass do something
        doBeforeCalculatingDerivedInfo();

        // calculate derived data for events themselves
        calculateTimestamps();
        calculateEventsInterval();
        calculateEventsMemoryInfo();

        // data in events should not change after this line
        // calculate specific data prepared for route api, order of these calls doesn't matter
        calculateCauseInfo();
        calculatePhaseInfo();
        calculateGCEventDetails();

        calculateKPI();
        calculateGcModelMetadata();

        // do some diagnoses
        progressListener.worked(250);
        progressListener.subTask("Diagnosing");
        diagnoseAndSuggest();
    }

    protected void doBeforeCalculatingDerivedInfo() {
    }

    private void decideAndFixEventInfo() {
        for (GCEvent event : gcEvents) {
            if ("System.gc".equals(event.getCause())) {
                event.setCause("System.gc()");
            }
            List<GCEvent> phases = event.getPhases();
            if (phases == null) {
                continue;
            }
            for (int i = phases.size() - 1; i >= 0; i--) {
                GCEvent phase = phases.get(i);
                if (phase.getDuration() == UNKNOWN_DOUBLE) {
                    //this is unlikely to happen, just give a reasonable value
                    phase.setDuration(phases.get(phases.size() - 1).getStartTime() - phase.getStartTime());
                }
            }
            if (event.getDuration() == UNKNOWN_DOUBLE && getStartTime() != UNKNOWN_DOUBLE) {
                event.setDuration(phases.get(phases.size() - 1).getEndTime() - event.getStartTime());
            }
        }
    }


    private void calculateTimestamps() {
        double referenceTimestamp = getReferenceTimestamp();
        if (referenceTimestamp == UNKNOWN_DOUBLE) {
            return;
        }
        for (GCEvent event : getGcEvents()) {
            event.setStartTimestamp(referenceTimestamp + event.getStartTime());
            if (event.hasPhases()) {
                for (GCEvent phase : event.getPhases()) {
                    phase.setStartTimestamp(referenceTimestamp + phase.getStartTime());
                }
            }
        }
    }

    private void calculateGCEventDetails() {
        // toString of events will not change in the future, cache them
        gcEventsDetailCache = new ArrayList<>();
        for (GCEvent event : gcEvents) {
            gcEventsDetailCache.add(event.toString());
        }
    }

    public boolean durationNotZero() {
        return getDuration() > EPS;
    }

    private void calculateKPI() {
        // Most kpi items are calculated in other calculate series functions for performance.
        // Here we just calculate the remaining.
        DoubleData pause = new DoubleData();
        LongData allocations = new LongData();
        LongData promotions = new LongData();
        for (GCEvent event : gcEvents) {
            if (event.getEventType().getPause() == GCPause.PAUSE && event.getEventType() != SAFEPOINT) {
                pause.add(event.getPause());
            } else if (event.getEventType().getPause() == GCPause.PARTIAL && event.getPhases() != null) {
                for (GCEvent phase : event.getPhases()) {
                    pause.add(phase.getPause());
                }
            }
            long promotion = event.getPromotion();
            if (promotion != UNKNOWN_INT) {
                promotions.add(promotion);
            }
        }
        for (GCEvent event : getGcCollectionEvents()) {
            long allocation = event.getAllocation();
            if (allocation != UNKNOWN_INT) {
                allocations.add(allocation);
            }
        }
    }

    private void diagnoseAndSuggest() {
        try {
            this.problemAndSuggestion = new GCDiagnoser(this).diagnose();
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    private static final String I18N_PROMOTION = "jifa.gclog.promotion";
    private static final List<String> PROMOTION_I18N_LIST = Arrays.asList(I18N_PROMOTION);

    private TimeLineChartView calculateGCMemoryPromotion(double startTime, double endTime, double bucketInterval) {
        List<TimeLineChartEvent> events = new ArrayList<>();
        for (GCEvent gcEvent : gcCollectionEvents) {
            if (gcEvent.getEndTime() < startTime) {
                continue;
            }
            if (gcEvent.getEndTime() > endTime) {
                break;
            }

            long promotion = gcEvent.getPromotion();
            if (promotion != UNKNOWN_INT) {
                events.add(new TimeLineChartEvent(I18N_PROMOTION, gcEvent.getEndTime(), promotion / KB2MB));
            }
        }
        return new TimeLineChartView.TimeLineChartViewBuilder()
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setEvents(events)
                .setLabels(PROMOTION_I18N_LIST)
                .setBucketInterval(bucketInterval)
                .setAverageByTime(true)
                .createByAggregation();
    }

    private static final String I18N_ALLOCATION = "jifa.gclog.allocation";
    private static final String I18N_RECLAMATION = "jifa.gclog.reclamation";
    private static final List<String> ALLO_REC_I18N_LIST = Arrays.asList(I18N_ALLOCATION, I18N_RECLAMATION);

    private TimeLineChartView calculateGCMemoryAlloRec(double startTime, double endTime, double bucketInterval) {
        List<TimeLineChartEvent> events = new ArrayList<>();
        // we will use zgc's own statistics to get allocation rate
        boolean useZGCStatistics = collectorType == ZGC && !((ZGCModel) this).getStatistics().isEmpty();
        for (GCEvent gcEvent : gcCollectionEvents) {
            if (gcEvent.getEndTime() < startTime) {
                continue;
            }
            if (gcEvent.getEndTime() > endTime) {
                break;
            }
            if (!useZGCStatistics) {
                long allocation = gcEvent.getAllocation();
                if (allocation != UNKNOWN_INT) {
                    events.add(new TimeLineChartEvent(I18N_ALLOCATION, gcEvent.getEndTime(), allocation / KB2MB / KB2MB));
                }
            }
            long reclamation = gcEvent.getReclamation();
            if (reclamation != UNKNOWN_INT) {
                events.add(new TimeLineChartEvent(I18N_RECLAMATION, gcEvent.getEndTime(), reclamation / KB2MB / KB2MB));
            }
        }
        if (collectorType == ZGC) {
            for (ZGCModel.ZStatistics statistic : ((ZGCModel) this).getStatistics()) {
                if (statistic.getStartTime() < startTime) {
                    continue;
                }
                if (statistic.getStartTime() > endTime) {
                    break;
                }
                events.add(new TimeLineChartEvent(I18N_ALLOCATION, statistic.getStartTime(),
                        statistic.get("Memory: Allocation Rate MB/s").getAvg10s() * 10));
            }
        }
        return new TimeLineChartView.TimeLineChartViewBuilder()
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setEvents(events)
                .setLabels(ALLO_REC_I18N_LIST)
                .setBucketInterval(bucketInterval)
                .setAverageByTime(true)
                .createByAggregation();
    }

    private static final String I18N_YOUNG = "jifa.gclog.youngRegion";
    private static final String I18N_OLD = "jifa.gclog.oldRegion";
    private static final String I18N_HUMONGOUS = "jifa.gclog.humongousRegion";
    private static final String I18N_HEAP_TOTAL = "jifa.gclog.totalHeap";
    private static final String I18N_HEAP_MAX = "jifa.gclog.heapMax";
    private static final List<String> HEAP_I18N_LIST_G1 = Arrays.asList(I18N_YOUNG, I18N_OLD, I18N_HUMONGOUS, I18N_HEAP_TOTAL, I18N_HEAP_MAX);
    private static final List<String> HEAP_I18N_LIST_OTHER = Arrays.asList(I18N_YOUNG, I18N_OLD, I18N_HEAP_TOTAL, I18N_HEAP_MAX);

    private TimeLineChartView calculateGCMemoryHeap(double startTime, double endTime) {
        List<TimeLineChartEvent> events = new ArrayList<>();
        for (GCEvent gcEvent : gcCollectionEvents) {
            if (gcEvent.getEndTime() < startTime) {
                continue;
            }
            if (gcEvent.getEndTime() > endTime) {
                break;
            }

            Map<HeapGeneration, GCCollectionResultItem> collection = gcEvent.getCollectionAgg();
            if (collection == null) {
                continue;
            }
            GCCollectionResultItem young = collection.get(YOUNG);
            GCCollectionResultItem old = collection.get(OLD);
            GCCollectionResultItem humongous = collection.get(HUMONGOUS);
            GCCollectionResultItem total = collection.get(TOTAL);
            if (young.getPreUsed() != UNKNOWN_INT) {
                events.add(new TimeLineChartEvent(I18N_YOUNG, gcEvent.getStartTime(),
                        (double) young.getPreUsed() / KB2MB));
                events.add(new TimeLineChartEvent(I18N_YOUNG, gcEvent.getEndTime(),
                        (double) young.getPostUsed() / KB2MB));
            }
            if (old.getPreUsed() != UNKNOWN_INT) {
                events.add(new TimeLineChartEvent(I18N_OLD, gcEvent.getStartTime(),
                        (double) old.getPreUsed() / KB2MB));
                events.add(new TimeLineChartEvent(I18N_OLD, gcEvent.getEndTime(),
                        (double) old.getPostUsed() / KB2MB));
            }
            if (humongous.getPreUsed() != UNKNOWN_INT) {
                events.add(new TimeLineChartEvent(I18N_HUMONGOUS, gcEvent.getStartTime(),
                        (double) humongous.getPreUsed() / KB2MB));
                events.add(new TimeLineChartEvent(I18N_HUMONGOUS, gcEvent.getEndTime(),
                        (double) humongous.getPostUsed() / KB2MB));
            }
            if (total.getPreUsed() != UNKNOWN_INT) {
                events.add(new TimeLineChartEvent(I18N_HEAP_TOTAL, gcEvent.getStartTime(),
                        (double) total.getPreUsed() / KB2MB));
                events.add(new TimeLineChartEvent(I18N_HEAP_TOTAL, gcEvent.getEndTime(),
                        (double) total.getPostUsed() / KB2MB));
            }
            if (total.getTotal() != UNKNOWN_INT) {
                events.add(new TimeLineChartEvent(I18N_HEAP_MAX, gcEvent.getStartTime(),
                        (double) total.getTotal() / KB2MB));
                events.add(new TimeLineChartEvent(I18N_HEAP_MAX, gcEvent.getEndTime(),
                        (double) total.getTotal() / KB2MB));
            }
        }
        List<String> labels = collectorType == G1 ? HEAP_I18N_LIST_G1 : HEAP_I18N_LIST_OTHER;
        return new TimeLineChartView.TimeLineChartViewBuilder()
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setEvents(events)
                .setLabels(labels)
                .createByPreservingData();
    }

    private static final String I18N_DURATION_PERCENTAGE = "jifa.gclog.gcOverview.gcDurationPercentage";
    private static final List<String> GCCYCLE_I18N_LIST = Arrays.asList(I18N_DURATION_PERCENTAGE);

    private TimeLineChartView calculateGCCycle(double startTime, double endTime) {
        // now this is only used in ZGC, may be used by other gcs in the future
        List<TimeLineChartEvent> events = new ArrayList<>();
        for (GCEvent gcEvent : gcCollectionEvents) {
            if (gcEvent.getEndTime() < startTime) {
                continue;
            }
            if (gcEvent.getEndTime() > endTime) {
                break;
            }
            if (gcEvent.getDuration() != UNKNOWN_DOUBLE && gcEvent.getInterval() != UNKNOWN_DOUBLE) {
                events.add(new TimeLineChartEvent(I18N_DURATION_PERCENTAGE, gcEvent.getEndTime(),
                        gcEvent.getDuration() / (gcEvent.getDuration() + gcEvent.getInterval()) * 100));
            }
        }
        return new TimeLineChartView.TimeLineChartViewBuilder()
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setEvents(events)
                .setLabels(GCCYCLE_I18N_LIST)
                .createByPreservingData();
    }

    private static final String I18N_METASPACE = "jifa.gclog.metaspaceRegion";
    private static final String I18N_METASPACE_MAX = "jifa.gclog.metaspaceMax";
    private static final List<String> METASPACE_I18N_LIST = Arrays.asList(I18N_METASPACE, I18N_METASPACE_MAX);

    private TimeLineChartView calculateGCMemoryMetaspace(double startTime, double endTime) {
        List<TimeLineChartEvent> events = new ArrayList<>();
        for (GCEvent gcEvent : gcCollectionEvents) {
            if (gcEvent.getEndTime() < startTime) {
                continue;
            }
            if (gcEvent.getEndTime() > endTime) {
                break;
            }

            Map<HeapGeneration, GCCollectionResultItem> collection = gcEvent.getCollectionAgg();
            GCCollectionResultItem metaspace = collection.get(METASPACE);
            if (metaspace.getPreUsed() != UNKNOWN_INT) {
                events.add(new TimeLineChartEvent(I18N_METASPACE, gcEvent.getStartTime(),
                        (double) metaspace.getPreUsed() / KB2MB));
            }
            if (metaspace.getPostUsed() != UNKNOWN_INT) {
                events.add(new TimeLineChartEvent(I18N_METASPACE, gcEvent.getEndTime(),
                        (double) metaspace.getPostUsed() / KB2MB));
            }
            if (metaspace.getTotal() != UNKNOWN_INT) {
                events.add(new TimeLineChartEvent(I18N_METASPACE_MAX, gcEvent.getStartTime(),
                        (double) metaspace.getTotal() / KB2MB));
                events.add(new TimeLineChartEvent(I18N_METASPACE_MAX, gcEvent.getEndTime(),
                        (double) metaspace.getTotal() / KB2MB));
            }
        }
        return new TimeLineChartView.TimeLineChartViewBuilder()
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setEvents(events)
                .setLabels(METASPACE_I18N_LIST)
                .createByPreservingData();
    }

    /**
     * calculate heap size(young, humongous, old, metaspace,total),
     * object allocation, reclamation and promotion
     */
    private void calculateEventsMemoryInfo() {
        for (GCEvent event : gcEvents) {
            calculateEventCollectionAgg(event);
        }
        gcCollectionEvents.sort(Comparator.comparingDouble(GCEvent::getStartTime));

        long lastTotalMemory = 0;
        for (GCEvent event : gcCollectionEvents) {
            Map<HeapGeneration, GCCollectionResultItem> collectionAgg = event.getCollectionAgg();
            GCCollectionResultItem young = collectionAgg.get(YOUNG);
            GCCollectionResultItem total = collectionAgg.get(TOTAL);
            GCCollectionResultItem humongous = collectionAgg.get(HUMONGOUS);
            // reclamation
            // sometimes it may have been calculated during parsing log
            if (event.getReclamation() == UNKNOWN_INT &&
                    total.getPreUsed() != UNKNOWN_INT && total.getPostUsed() != UNKNOWN_INT) {
                event.setReclamation(total.getPreUsed() - total.getPostUsed());
            }
            // promotion
            if (event.getPromotion() == UNKNOWN_INT &&
                    event.hasPromotion() && event.getEventType() != G1_YOUNG_MIXED_GC) {
                // notice: g1 young mixed gc should have promotion, but we have no way to know it exactly
                long youngReduction = young.getMemoryReduction();
                long totalReduction = total.getMemoryReduction();
                if (youngReduction != UNKNOWN_INT && totalReduction != UNKNOWN_INT) {
                    long promotion = youngReduction - totalReduction;
                    long humongousReduction = humongous.getMemoryReduction();
                    if (humongousReduction != UNKNOWN_INT) {
                        promotion -= humongousReduction;
                    }
                    event.setPromotion(promotion);
                }
            }
            // allocation
            if (event.getAllocation() == UNKNOWN_INT && total.getPreUsed() != UNKNOWN_INT) {
                // As to concurrent event, allocation is composed of two parts: allocation between two adjacent events
                // and during event. If original allocation is not unknown, that value is allocation during event.
                event.setAllocation(zeroIfUnknownInt(event.getAllocation()) + total.getPreUsed() - lastTotalMemory);
                lastTotalMemory = total.getPostUsed();
            }
        }
    }

    private long zeroIfUnknownInt(long x) {
        return x == UNKNOWN_INT ? 0 : x;
    }

    private void calculateEventCollectionAgg(GCEvent event) {
        if (event.hasPhases()) {
            for (GCEvent phase : event.getPhases()) {
                calculateEventCollectionAgg(phase);
            }
        }

        GCCollectionResult collectionResult = event.getCollectionResult();
        if (collectionResult == null) {
            return;
        }
        gcCollectionEvents.add(event);
        // just consider young old and metaspace areas, and total of young and old
        // sometimes total != young + old because of rounding error
        // sometimes total is known but young/old are known
        Map<HeapGeneration, GCCollectionResultItem> collectionAgg = new HashMap<>();
        event.setCollectionAgg(collectionAgg);

        // design pattern: Null Object
        HeapGeneration[] generations = {YOUNG, OLD, HUMONGOUS, METASPACE, TOTAL};
        for (HeapGeneration generation : generations) {
            collectionAgg.put(generation, new GCCollectionResultItem(generation));
        }

        if (collectionResult.getSummary() != null) {
            collectionAgg.put(HeapGeneration.TOTAL, collectionResult.getSummary());
        }
        if (collectionResult.getItems() != null) {
            for (GCCollectionResultItem item : collectionResult.getItems()) {
                HeapGeneration generation = item.getGeneration();
                if (generation == EDEN || generation == SURVIVOR) {
                    generation = YOUNG;
                }
                GCCollectionResultItem newItem = item.mergeIfPresent(collectionAgg.get(generation));
                newItem.setGeneration(generation);
                collectionAgg.put(generation, newItem);
            }
            // sometimes we know partial info and we can infer remaining
            calculateRemainingFromCollectionAggregation(collectionAgg);
        }
    }

    private void calculateRemainingFromCollectionAggregation(Map<HeapGeneration, GCCollectionResultItem> collectionAgg) {
        //case 1: know young and old, calculate total
        GCCollectionResultItem total = collectionAgg.get(YOUNG)
                .merge(collectionAgg.get(OLD))
                .mergeIfPresent(collectionAgg.get(HUMONGOUS));
        total.setGeneration(TOTAL);
        collectionAgg.put(TOTAL, collectionAgg.get(TOTAL).updateIfAbsent(total));

        //case 2: know old and total, calculate young
        GCCollectionResultItem young = collectionAgg.get(TOTAL)
                .subtract(collectionAgg.get(OLD))
                .subtractIfPresent(collectionAgg.get(HUMONGOUS));
        young.setGeneration(YOUNG);
        collectionAgg.put(YOUNG, collectionAgg.get(YOUNG).updateIfAbsent(young));

        //case 3: know young and total, calculate old
        GCCollectionResultItem old = collectionAgg.get(TOTAL)
                .subtract(collectionAgg.get(YOUNG))
                .subtractIfPresent(collectionAgg.get(HUMONGOUS));
        old.setGeneration(OLD);
        collectionAgg.put(OLD, collectionAgg.get(OLD).updateIfAbsent(old));
    }

    private void filterInvalidEvents() {
        // Sometimes the given log is just a part of the complete log. This may lead to some incomplete events at
        // beginning or end of this log. Such event at beginning is likely to have been dealt by parser, so here we try
        // to deal with the last event
        if (gcEvents.get(gcEvents.size() - 1).getEndTime() == UNKNOWN_DOUBLE) {
            gcEvents.remove(gcEvents.size() - 1);
        }
    }

    private TimeLineChartView calculateGCOverviewCount(double startTime, double endTime, double bucketInterval) {
        List<TimeLineChartEvent> chartEvents = new ArrayList<>();
        for (GCEvent event : gcEvents) {
            if (event.getEventType() == SAFEPOINT || event.getEndTime() < startTime) {
                continue;
            }
            if (event.getEndTime() > endTime) {
                break;
            }
            chartEvents.add(new TimeLineChartEvent(event.getEventType().getName(), event.getEndTime(), 1));
        }
        List<String> labels = getMetadataEventTypes();
        return new TimeLineChartView.TimeLineChartViewBuilder()
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setEvents(chartEvents)
                .setLabels(labels)
                .setBucketInterval(bucketInterval)
                .createByAggregation();
    }

    public TimeLineChartView calculateGCOverviewPause(double startTime, double endTime) {
        List<TimeLineChartEvent> chartEvents = new ArrayList<>();
        List<String> labels = getPauseEventNames();
        for (GCEvent event : gcEvents) {
            if (event.getEventType() == SAFEPOINT || event.getEndTime() < startTime) {
                continue;
            }
            if (event.getEndTime() > endTime) {
                break;
            }
            if (event.getEventType().getPause() == GCPause.PAUSE) {
                chartEvents.add(new TimeLineChartEvent(event.getEventType().getName(), event.getEndTime(), event.getPause()));
            }
            if (event.getEventType().getPause() == GCPause.PARTIAL && event.getPhases() != null) {
                for (GCEvent phase : event.getPhases()) {
                    if (labels.contains(phase.getEventType().getName())) {
                        chartEvents.add(new TimeLineChartEvent(phase.getEventType().getName(), phase.getEndTime(), phase.getPause()));
                    }
                }
            }
        }
        return new TimeLineChartView.TimeLineChartViewBuilder()
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setEvents(chartEvents)
                .setLabels(labels)
                .createByPreservingData();
    }

    protected abstract List<String> getPauseEventNames();

    public GCEvent getLastEventWithCondition(Predicate<GCEvent> condition) {
        for (int i = allEvents.size() - 1; i >= 0; i--) {
            GCEvent event = allEvents.get(i);
            if (condition.test(event)) {
                return event;
            }
        }
        return null;
    }

    // mainly used in jdk8, where gcid may be missing
    public GCEvent getLastEventOfType(GCEventType... types) {
        List<GCEventType> typeList = Arrays.asList(types);
        return getLastEventWithCondition(event -> typeList.contains(event.getEventType()));
    }

    // mainly used in parser of jdk11, where gcid is always logged if tag includes gc
    public GCEvent getLastEventOfGCID(int gcid) {
        return getLastEventWithCondition(event -> event.getEventLevel() == GCEventLevel.EVENT && event.getGcid() == gcid);
    }

    public double getReferenceTimestamp() {
        return referenceTimestamp;
    }

    public void setReferenceTimestamp(double referenceTimestamp) {
        this.referenceTimestamp = referenceTimestamp;
    }

    public void setVmOptions(VmOptions vmOptions) {
        this.vmOptions = vmOptions;
    }

    public List<ProblemAndSuggestion> getProblemAndSuggestion() {
        return problemAndSuggestion;
    }

    public VmOptions getVmOptions() {
        return vmOptions;
    }

    private void calculateEventsInterval() {
        Map<GCEventType, Double> lastEndTime = new HashMap<>();
        for (GCEvent event : gcEvents) {
            GCEventType eventType = event.getEventType();
            // don't distinguish young GC
            if (event.isYoungGC()) {
                eventType = YOUNG_GC;
            }
            if (lastEndTime.containsKey(eventType)) {
                event.setInterval(Math.max(0, event.getStartTime() - lastEndTime.get(eventType)));
            }
            lastEndTime.put(eventType, event.getEndTime());
            if (event.hasPhases()) {
                for (GCEvent phase : event.getPhases()) {
                    GCEventType phaseType = phase.getEventType();
                    if (lastEndTime.containsKey(phaseType)) {
                        phase.setInterval(phase.getStartTime() - lastEndTime.get(phaseType));
                    }
                    lastEndTime.put(phaseType, phase.getStartTime());
                }
            }
        }
    }

    protected abstract List<GCEventType> getSupportedPhaseEventTypes();

    private void calculatePhaseInfo() {
        //DoubleData[] is array with length 3 that records duration, interval and pause
        List<GCEventType> phaseInfoPhases = getSupportedPhaseEventTypes();
        Map<GCEventType, DoubleData[]> gcPhaseTimeMap = new HashMap<>();
        for (GCEvent event : gcEvents) {
            putDurationIntervalPause(gcPhaseTimeMap, event);
            //phases
            if (!event.hasPhases()) {
                continue;
            }
            for (GCEvent phase : event.getPhases()) {
                if (phaseInfoPhases.contains(phase.getEventType()))
                    putDurationIntervalPause(gcPhaseTimeMap, phase);
            }
        }

        gcPhaseInfos = new ArrayList<>();
        for (GCEventType phase : phaseInfoPhases) {
            if (gcPhaseTimeMap.containsKey(phase)) {
                DoubleData[] doubleDatas = gcPhaseTimeMap.get(phase);
                DoubleData durations = doubleDatas[0];
                DoubleData intervals = doubleDatas[1];
                DoubleData pauses = doubleDatas[2];
                gcPhaseInfos.add(new GCPhaseInfo(phase.getName(),
                        durations.getN(),
                        durations.average(),
                        durations.getMax(),
                        durations.getSum(),
                        intervals.getN() == 0 ? UNKNOWN_DOUBLE : intervals.average(),
                        phase.getPause() == GCPause.PAUSE
                ));
            }
        }
    }

    public String toDebugString() {
        StringBuilder sb = new StringBuilder();
        for (GCEvent event : gcEvents) {
            sb.append(event).append("\n");
            if (event.hasPhases()) {
                for (GCEvent phase : event.getPhases()) {
                    sb.append("         ").append(phase).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private void putDurationIntervalPause(Map<GCEventType, DoubleData[]> map, GCEvent event) {
        if (!map.containsKey(event.getEventType())) {
            map.put(event.getEventType(), new DoubleData[]{new DoubleData(), new DoubleData(), new DoubleData()});
        }
        DoubleData[] doubleDatas = map.get(event.getEventType());
        if (event.getDuration() != UNKNOWN_DOUBLE) {
            doubleDatas[0].add(event.getDuration());
        }
        if (event.getInterval() != UNKNOWN_DOUBLE) {
            doubleDatas[1].add(event.getInterval());
        }
        if (event.getPause() != UNKNOWN_DOUBLE) {
            doubleDatas[2].add(event.getPause());
        }
    }

    private void calculateCauseInfo() {
        Map<String, DoubleData> gcCausePauseMap = new HashMap<>();
        for (GCEvent event : gcEvents) {
            double pause = event.getPause();
            String cause = event.getCause();
            if (pause == UNKNOWN_DOUBLE) {
                continue;
            }
            if (cause != null) {
                cause = (event.getEventType() == FULL_GC ? FULL_GC.getName() : YOUNG_GC.getName()) + " - " + cause;
                if (!gcCausePauseMap.containsKey(cause)) {
                    gcCausePauseMap.put(cause, new DoubleData());
                }
                gcCausePauseMap.get(cause).add(pause);
            }
            // regard some special situations as causes as well, though they are not defined as cause in Hotspot
            if (event.getSpecialSituations() != null) {
                for (GCSpecialSituation situation : event.getSpecialSituations()) {
                    if (situation == GCSpecialSituation.PROMOTION_FAILED || situation == GCSpecialSituation.TO_SPACE_EXHAUSTED) {
                        cause = situation.getName();
                        if (!gcCausePauseMap.containsKey(cause)) {
                            gcCausePauseMap.put(cause, new DoubleData());
                        }
                        gcCausePauseMap.get(cause).add(pause);
                    }
                }
            }
        }
    }

    public List<String> getGCDetails() {
        return gcEventsDetailCache;
    }

    public PageView<String> getGCDetails(PagingRequest pagingRequest, GCDetailFilter filter) {
        List<String> details = getGCDetails();
        List<String> filtered = new ArrayList<>();
        for (int i = 0; i < details.size(); i++) {
            if (!filter.isFiltered(gcEvents.get(i))) {
                filtered.add(details.get(i));
            }
        }
        List<String> result = new ArrayList<>();
        int firstIndex = (pagingRequest.getPage() - 1) * pagingRequest.getPageSize();
        int lastIndex = Math.min(firstIndex + pagingRequest.getPageSize(), filtered.size());
        for (int i = firstIndex; i < lastIndex; i++) {
            result.add(filtered.get(i));
        }
        return new PageView<>(pagingRequest, filtered.size(), result);
    }

    abstract protected List<String> getMetadataEventTypes();

    public GCLogMetadata getGcModelMetadata() {
        return metadata;
    }

    private void calculateGcModelMetadata() {
        Set<String> causes = new HashSet<>();
        for (GCEvent gcEvent : getGcEvents()) {
            if (gcEvent.getCause() != null) {
                causes.add(gcEvent.getCause());
            }
        }
        metadata = new GCLogMetadata();
        metadata.setCauses(new ArrayList<>(causes));
        metadata.setCollector(getCollectorType());
        metadata.setLogStyle(logStyle);
        metadata.setPauseless(isPauseless());
        metadata.setGenerational(isGenerational());
        metadata.setTimestamp(getReferenceTimestamp());
        metadata.setStartTime(getStartTime());
        metadata.setEndTime(getEndTime());
        metadata.setEventTypes(getMetadataEventTypes());
    }

    public TimeLineChartView getGraphView(String type, double timeSpan, double timePoint) {
        double[] chartStartEndTime = decideGraphStartEndTime(timePoint, timeSpan);
        chartStartEndTime[0] = Math.floor(chartStartEndTime[0] / MS2S) * MS2S;
        chartStartEndTime[1] = Math.ceil(chartStartEndTime[1] / MS2S) * MS2S;
        double bucketInterval = getBucketInterval(timeSpan);
        switch (type) {
            case "count":
                return calculateGCOverviewCount(chartStartEndTime[0], chartStartEndTime[1], bucketInterval);
            case "pause":
                return calculateGCOverviewPause(chartStartEndTime[0], chartStartEndTime[1]);
            case "heap":
                return calculateGCMemoryHeap(chartStartEndTime[0], chartStartEndTime[1]);
            case "metaspace":
                return calculateGCMemoryMetaspace(chartStartEndTime[0], chartStartEndTime[1]);
            case "alloRec":
                return calculateGCMemoryAlloRec(chartStartEndTime[0], chartStartEndTime[1], bucketInterval);
            case "promotion":
                return calculateGCMemoryPromotion(chartStartEndTime[0], chartStartEndTime[1], bucketInterval);
            case "gccycle":
                return calculateGCCycle(chartStartEndTime[0], chartStartEndTime[1]);
            default:
                ErrorUtil.shouldNotReachHere();
        }
        return null;
    }

    private double getBucketInterval(double span) {
        // hard code here
        switch ((int) span) {
            case 300000:
                return MS2S;
            case 3600000:
                return 30 * MS2S;
            case 10800000:
                return 60 * MS2S;
            case 43200000:
                return 120 * MS2S;
            case 259200000:
                return 600 * MS2S;
            default:
                ErrorUtil.shouldNotReachHere();
        }
        return 0;
    }

    private double[] decideGraphStartEndTime(double timePoint, double timeSpan) {
        double start = timePoint - timeSpan / 2;
        double end = timePoint + timeSpan / 2;
        if (start < getStartTime()) {
            return new double[]{getStartTime(), getStartTime() + Math.min(getDuration(), timeSpan)};
        }
        if (end > getEndTime()) {
            return new double[]{getEndTime() - Math.min(getDuration(), timeSpan), getEndTime()};
        }
        return new double[]{start, end};
    }

    public void setParallelThread(int parallelThread) {
        this.parallelThread = parallelThread;
    }

    public void setConcurrentThread(int concurrentThread) {
        this.concurrentThread = concurrentThread;
    }

    public int getParallelThread() {
        return parallelThread;
    }

    public int getConcurrentThread() {
        return concurrentThread;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GCPhaseInfo {
        private String name;
        private int count;
        private double avgTime;
        private double maxTime;
        private double totalTime;
        private double avgInterval;
        private boolean stw;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemAndSuggestion {
        private I18nStringView problem;
        private List<I18nStringView> suggestions = new ArrayList<>(4);

        public void addSuggestion(I18nStringView suggestion) {
            suggestions.add(suggestion);
        }

        public ProblemAndSuggestion(I18nStringView problem, I18nStringView suggestion) {
            this.problem = problem;
            this.suggestions.add(suggestion);
        }

        public ProblemAndSuggestion(I18nStringView problem, I18nStringView... suggestion) {
            this.problem = problem;
            Collections.addAll(suggestions, suggestion);
        }
    }

    @Data
    @NoArgsConstructor
    @ToString
    public static class GCDetailFilter {
        private String eventType;
        private String gcCause;
        //in ms
        private double logTimeLow;
        private double logTimeHigh;
        private double pauseTimeLow;

        public GCDetailFilter(String eventType, String gcCause, Double logTimeLow, Double logTimeHigh, Double pauseTimeLow) {
            this.eventType = eventType;
            this.gcCause = gcCause;
            this.logTimeLow = logTimeLow == null ? -Double.MAX_VALUE : logTimeLow;
            this.logTimeHigh = logTimeHigh == null ? Double.MAX_VALUE : logTimeHigh;
            this.pauseTimeLow = pauseTimeLow == null ? -Double.MAX_VALUE : pauseTimeLow;
        }

        public boolean isFiltered(GCEvent event) {
            return event.getEventType() == SAFEPOINT ||
                    !((eventType == null || eventType.equals(event.getEventType().getName()))
                            && (gcCause == null || gcCause.equals(event.getCause()))
                            && (logTimeLow <= event.getEndTime() && event.getEndTime() <= logTimeHigh)
                            && (pauseTimeLow <= event.getPause()));
        }
    }

}

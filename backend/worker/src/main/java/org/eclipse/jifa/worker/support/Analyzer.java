/********************************************************************************
 * Copyright (c) 2020, 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.worker.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.vertx.core.Promise;
import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.common.enums.ProgressState;
import org.eclipse.jifa.common.listener.DefaultProgressListener;
import org.eclipse.jifa.common.listener.ProgressListener;
import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.common.util.FileUtil;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.parser.GCLogAnalyzer;
import org.eclipse.jifa.hda.api.HeapDumpAnalyzer;
import org.eclipse.jifa.tda.ThreadDumpAnalyzer;
import org.eclipse.jifa.worker.Worker;
import org.eclipse.jifa.worker.WorkerGlobal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import static org.eclipse.jifa.common.enums.FileType.GC_LOG;
import static org.eclipse.jifa.common.enums.FileType.HEAP_DUMP;
import static org.eclipse.jifa.common.enums.FileType.THREAD_DUMP;
import static org.eclipse.jifa.common.util.Assertion.ASSERT;
import static org.eclipse.jifa.worker.Constant.CacheConfig.CACHE_CONFIG;
import static org.eclipse.jifa.worker.Constant.CacheConfig.EXPIRE_AFTER_ACCESS;
import static org.eclipse.jifa.worker.Constant.CacheConfig.EXPIRE_AFTER_ACCESS_TIME_UNIT;

public class Analyzer {

    public static final HeapDumpAnalyzer.Provider HEAP_DUMP_ANALYZER_PROVIDER;
    private static final Logger LOGGER = LoggerFactory.getLogger(Analyzer.class);

    static {
        try {
            Iterator<HeapDumpAnalyzer.Provider> iterator =
                ServiceLoader.load(HeapDumpAnalyzer.Provider.class, Worker.class.getClassLoader()).iterator();
            ASSERT.isTrue(iterator.hasNext());
            HEAP_DUMP_ANALYZER_PROVIDER = iterator.next();
        } catch (Throwable t) {
            LOGGER.error("Init analyzer failed", t);
            throw new Error(t);
        }
    }

    private final Map<String, ProgressListener> listeners;
    private final Cache<String, Object> cache;

    private Analyzer() {
        listeners = new HashMap<>();
        cache = Caffeine
                .newBuilder()
                .scheduler(Scheduler.systemScheduler())
                .softValues()
                .expireAfterAccess(
                        WorkerGlobal.intConfig(CACHE_CONFIG, EXPIRE_AFTER_ACCESS),
                        TimeUnit.valueOf(WorkerGlobal.stringConfig(CACHE_CONFIG, EXPIRE_AFTER_ACCESS_TIME_UNIT))
                                  )
                .removalListener((k, v, removalCause) -> {
                    LOGGER.info("Clear cache: {}", k);
                    if (v instanceof HeapDumpAnalyzer) {
                        ((HeapDumpAnalyzer) v).dispose();
                    }

                })
                .build();
    }

    private static <T> T getOrBuild(String key, Builder<T> builder) {
        T result = getInstance().getCacheValueIfPresent(key);
        if (result != null) {
            return result;
        }

        synchronized (key.intern()) {
            result = getInstance().getCacheValueIfPresent(key);
            if (result != null) {
                return result;
            }
            try {
                result = builder.build(key);
            } catch (Throwable t) {
                throw new JifaException(t);
            }
            getInstance().putCacheValue(key, result);
            return result;
        }
    }

    public static HeapDumpAnalyzer getOrBuildHeapDumpAnalyzer(String dump, Map<String, String> options,
                                                              ProgressListener listener) {
        return getOrBuild(dump, key -> HEAP_DUMP_ANALYZER_PROVIDER
            .provide(new File(FileSupport.filePath(HEAP_DUMP, dump)).toPath(), options, listener));
    }

    public static Analyzer getInstance() {
        return Singleton.INSTANCE;
    }

    public boolean isFirstAnalysis(FileType fileType, String file) {
        switch (fileType) {
            case HEAP_DUMP:
                return !new File(FileSupport.indexPath(fileType, file)).exists() &&
                       !new File(FileSupport.errorLogPath(fileType, file)).exists() &&
                       getFileListener(file) == null;
            default:
                throw new IllegalArgumentException(fileType.name());
        }
    }

    public void analyze(Promise<Void> promise, FileType fileType, String fileName, Map<String, String> options) {
        ProgressListener progressListener;

        if (getCacheValueIfPresent(fileName) != null ||
            new File(FileSupport.errorLogPath(fileType, fileName)).exists()) {
            promise.complete();
            return;
        }

        progressListener = new DefaultProgressListener();
        boolean success = putFileListener(fileName, progressListener);
        promise.complete();

        if (success) {
            try {
                switch (fileType) {
                    case HEAP_DUMP:
                        getOrBuildHeapDumpAnalyzer(fileName, options, progressListener);
                        break;
                    case GC_LOG:
                        getOrOpenGCLogModel(fileName,progressListener);
                        break;
                    case THREAD_DUMP:
                        threadDumpAnalyzerOf(fileName, progressListener);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                LOGGER.error("task failed due to {}", ErrorUtil.toString(e));
                LOGGER.error(progressListener.log());
                File log = new File(FileSupport.errorLogPath(fileType, fileName));
                FileUtil.write(log, progressListener.log(), false);
                FileUtil.write(log, ErrorUtil.toString(e), true);
            } finally {
                removeFileListener(fileName);
            }
        }
    }

    public void clean(FileType fileType, String fileName) {
        clearCacheValue(fileName);

        File errorLog = new File(FileSupport.errorLogPath(fileType, fileName));
        if (errorLog.exists()) {
            ASSERT.isTrue(errorLog.delete(), "Delete error log failed");
        }

        if (getFileListener(fileName) != null) {
            return;
        }

        File index = new File(FileSupport.indexPath(fileType, fileName));
        if (index.exists()) {
            ASSERT.isTrue(index.delete(), "Delete index file failed");
        }

        File kryo = new File(FileSupport.filePath(fileType, fileName, fileName + ".kryo"));
        if (kryo.exists()) {
            ASSERT.isTrue(kryo.delete(), "Delete kryo file failed");
        }
    }

    public void release(String fileName) {
        clearCacheValue(fileName);
    }

    public org.eclipse.jifa.common.vo.Progress pollProgress(FileType fileType, String fileName) {
        ProgressListener progressListener = getFileListener(fileName);

        if (progressListener == null) {
            org.eclipse.jifa.common.vo.Progress progress = buildProgressIfFinished(fileType, fileName);
            return progress;
        } else {
            org.eclipse.jifa.common.vo.Progress progress = new org.eclipse.jifa.common.vo.Progress();
            progress.setState(ProgressState.IN_PROGRESS);
            progress.setMessage(progressListener.log());
            progress.setPercent(progressListener.percent());
            return progress;
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized <T> T getCacheValueIfPresent(String key) {
        return (T) cache.getIfPresent(key);
    }

    private synchronized void putCacheValue(String key, Object value) {
        cache.put(key, value);
        LOGGER.info("Put cache: {}", key);
    }

    private synchronized void clearCacheValue(String key) {
        cache.invalidate(key);
        LOGGER.info("Clear cache: {}", key);
    }

    private synchronized ProgressListener getFileListener(String fileName) {
        return listeners.get(fileName);
    }

    private synchronized boolean putFileListener(String fileName, ProgressListener listener) {
        if (listeners.containsKey(fileName)) {
            return false;
        }
        listeners.put(fileName, listener);
        return true;
    }

    private synchronized void removeFileListener(String fileName) {
        listeners.remove(fileName);
    }

    private org.eclipse.jifa.common.vo.Progress buildProgressIfFinished(FileType fileType, String fileName) {
        if (getCacheValueIfPresent(fileName) != null) {
            org.eclipse.jifa.common.vo.Progress result = new org.eclipse.jifa.common.vo.Progress();
            result.setPercent(1);
            result.setState(ProgressState.SUCCESS);
            return result;
        }

        File failed = new File(FileSupport.errorLogPath(fileType, fileName));
        if (failed.exists()) {
            org.eclipse.jifa.common.vo.Progress result = new org.eclipse.jifa.common.vo.Progress();
            result.setState(ProgressState.ERROR);
            result.setMessage(FileUtil.content(failed));
            return result;
        }

        org.eclipse.jifa.common.vo.Progress result = new org.eclipse.jifa.common.vo.Progress();
        result.setState(ProgressState.NOT_STARTED);
        return result;
    }

    interface Builder<T> {
        T build(String key) throws Throwable;
    }

    private static class Singleton {
        static Analyzer INSTANCE = new Analyzer();
    }

    public static GCModel getOrOpenGCLogModel(String info) {
        return getOrOpenGCLogModel(info, ProgressListener.NoOpProgressListener);
    }

    private static GCModel getOrOpenGCLogModel(String gclogFile, ProgressListener listener) {
        return getOrBuild(gclogFile,
                key -> new GCLogAnalyzer(new File(FileSupport.filePath(GC_LOG,  gclogFile)),
                        listener).parse());
    }

    public static ThreadDumpAnalyzer threadDumpAnalyzerOf(String threadDumpFile) {
        return threadDumpAnalyzerOf(threadDumpFile, ProgressListener.NoOpProgressListener);
    }

    public static ThreadDumpAnalyzer threadDumpAnalyzerOf(String threadDumpFile,
                                                          ProgressListener listener) {
        return getOrBuild(threadDumpFile,
                          key -> ThreadDumpAnalyzer
                              .build(new File(FileSupport.filePath(THREAD_DUMP, threadDumpFile)).toPath(), listener));
    }
}

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

package org.eclipse.jifa.tda.parser;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jifa.common.listener.ProgressListener;
import org.eclipse.jifa.tda.enums.JavaThreadState;
import org.eclipse.jifa.tda.enums.MonitorState;
import org.eclipse.jifa.tda.enums.OSTreadState;
import org.eclipse.jifa.tda.enums.SourceType;
import org.eclipse.jifa.tda.enums.ThreadType;
import org.eclipse.jifa.tda.model.ConcurrentLock;
import org.eclipse.jifa.tda.model.Frame;
import org.eclipse.jifa.tda.model.JavaThread;
import org.eclipse.jifa.tda.model.Monitor;
import org.eclipse.jifa.tda.model.Pool;
import org.eclipse.jifa.tda.model.RawMonitor;
import org.eclipse.jifa.tda.model.Snapshot;
import org.eclipse.jifa.tda.model.Thread;
import org.eclipse.jifa.tda.model.Trace;
import org.eclipse.jifa.tda.util.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JStackParser implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JStackParser.class);

    private static final BlockingDeque<ParserImpl.RawJavaThread> QUEUE;

    private static final ExecutorService ES;

    static {
        QUEUE = new LinkedBlockingDeque<>(128);

        int count = Math.max(2, Runtime.getRuntime().availableProcessors());
        ES = Executors.newFixedThreadPool(count);
        assert count >= 2;

        for (int i = 0; i < count; i++) {
            ES.submit(() -> {
                //noinspection InfiniteLoopStatement
                while (true) {
                    try {
                        QUEUE.take().parse();
                    } catch (Throwable t) {
                        LOGGER.error("Parse one thread error", t);
                    }
                }
            });
        }
    }

    @Override
    public Snapshot parse(Path path, ProgressListener listener) {
        try {
            Snapshot snapshot = new ParserImpl(path, listener).parse();
            snapshot.post();
            return snapshot;
        } catch (Throwable t) {
            if (t instanceof ParserException) {
                throw (ParserException) t;
            }
            throw new ParserException(t);
        }
    }

    static final class PATTERNS {
        static String TIME_FORMAT;
        static Pattern TIME;

        static Pattern VERSION;

        static String SMR_HEAD;

        static Pattern JAVA_THREAD;

        static Pattern JAVA_STATE;

        static Pattern JAVA_FRAME;

        static Pattern NO_JAVA_THREAD;

        static Pattern JNI_GLOBAL_REFS;

        static Pattern WAITING_ON;
        static Pattern WAITING_TO_RE_LOCK;
        static Pattern PARKING;
        static Pattern WAITING_ON_CLASS_INITIALIZATION;

        static Pattern LOCKED;
        static Pattern WAITING_TO_LOCK;
        static Pattern ELIMINATED_SCALAR_REPLACED;
        static Pattern ELIMINATED;

        static String LOCKED_OWNABLE_SYNCHRONIZERS;
        static String NONE;
        static Pattern LOCKED_SYNCHRONIZER;

        static String DEAD_LOCK_HEAD;
        static Pattern DEAD_LOCK_THREAD;
        static Pattern DEAD_LOCK_WAITING_TO_LOCK_MONITOR;
        static Pattern DEAD_LOCK_WAITING_TO_LOCK_SYNCHRONIZER;
        static Pattern DEAD_HELD_INFO;
        static String DEAD_LOCK_STACK_HEAD;
        static Pattern DEAD_FOUND;

        static {
            try {
                String fn = "jstack_pattern.properties";
                Properties ps = new Properties();
                ps.load(PATTERNS.class.getClassLoader().getResourceAsStream(fn));
                Field[] fields = PATTERNS.class.getDeclaredFields();
                for (Field field : fields) {
                    String value = (String) ps.get(field.getName());
                    if (value == null) {
                        throw new ParserException(field.getName() + " not found in " + fn);
                    }
                    Class<?> type = field.getType();
                    if (type == Pattern.class) {
                        field.set(null, Pattern.compile((String) ps.get(field.getName())));
                    } else if (type == String.class) {
                        field.set(null, value);
                    }
                }
            } catch (Throwable t) {
                if (t instanceof ParserException) {
                    throw (ParserException) t;
                }
                throw new ParserException(t);
            }
        }

        static Pattern patternOf(ParserImpl.Element element) {
            switch (element) {
                case TIME:
                    return TIME;
                case VERSION:
                    return VERSION;
                case JNI_GLOBAL_REFS:
                    return JNI_GLOBAL_REFS;
                case NON_JAVA_THREAD:
                    return NO_JAVA_THREAD;
                default:
                    throw new ParserException("Should not reach here");
            }
        }
    }

    private static class ParserImpl {

        private final Input input;

        private final AtomicInteger processing;

        private final List<Throwable> errors;

        private final Snapshot snapshot;

        private final ProgressListener listener;

        ParserImpl(Path path, ProgressListener listener) throws IOException {
            this.input = new Input(path);
            this.listener = listener;
            snapshot = new Snapshot();
            snapshot.setPath(path.toAbsolutePath().toString());
            processing = new AtomicInteger(0);
            errors = new ArrayList<>();

            step();
        }

        Snapshot parse() throws Exception {
            listener.beginTask("Parsing thread dump", 100);
            listener.subTask("Parsing timestamp and version");
            parseTimeStamp();
            parseVersion();
            listener.worked(1);

            skipSMR();

            // concurrent
            listener.subTask("Parsing threads");
            parseThreads();

            listener.subTask("Parsing JNI handles");
            parseJNIGlobalHandles();
            listener.worked(1);
            listener.subTask("Parsing JNI deadLocks");
            parseDeadLocks();
            listener.worked(8);

            // Wait for all Java threads to complete
            synchronized (this) {
                while (processing.get() != 0) {
                    this.wait();
                }
            }
            listener.worked(90);

            return snapshot;
        }

        void step() throws IOException {
            String line;
            while ((line = input.readLine()) != null) {
                if (!StringUtils.isBlank(line)) {
                    return;
                }
            }
        }

        void skipSMR() throws IOException {
            if (PATTERNS.SMR_HEAD.equals(input.currentLine())) {
                //noinspection StatementWithEmptyBody
                while (StringUtils.isNotBlank(input.readLine()))
                    ;
            }
        }

        void parseByElementPattern(Element element, Action action, boolean stepOnFailed) throws Exception {
            String line = input.currentLine();
            if (line == null) {
                LOGGER.warn("Skip parsing {} caused by EOF", element.description);
                return;
            }
            Matcher matcher = PATTERNS.patternOf(element).matcher(line);
            if (matcher.matches()) {
                try {
                    action.onMatched(matcher);
                } finally {
                    step();
                }
            } else {
                LOGGER.warn("Parse {} failed: {}", element.description, line);
                if (stepOnFailed) {
                    step();
                }
            }
        }

        void parseTimeStamp() throws Exception {
            parseByElementPattern(Element.TIME, m -> {
                long ts = new SimpleDateFormat(PATTERNS.TIME_FORMAT).parse(input.currentLine()).getTime();
                snapshot.setTimestamp(ts);
            }, false);
        }

        void parseVersion() throws Exception {
            parseByElementPattern(Element.VERSION, m -> {
                snapshot.setVmInfo(m.group("info"));
            }, false);
        }

        void parseJNIGlobalHandles() throws Exception {
            parseByElementPattern(Element.JNI_GLOBAL_REFS, m -> {
                String all = m.group("all");
                if (all != null) {
                    snapshot.setJniRefs(Integer.parseInt(all));
                } else {
                    int strong = Integer.parseInt(m.group("strong"));
                    int weak = Integer.parseInt(m.group("weak"));
                    snapshot.setJniRefs(strong + weak);
                    snapshot.setJniWeakRefs(weak);
                }
            }, false);
        }

        void parseDeadLocks() throws Exception {
            String line = input.currentLine();
            if (line == null) {
                return;
            }
            int dlCount = 0;
            while (line.equals(PATTERNS.DEAD_LOCK_HEAD)) {
                dlCount++;
                if (snapshot.getDeadLockThreads() == null) {
                    snapshot.setDeadLockThreads(new ArrayList<>());
                }
                List<JavaThread> threads = new ArrayList<>();
                // skip ====
                input.readLine();
                step();

                line = input.currentLine();
                Matcher matcher;

                int tCount = 0;
                do {
                    matcher = PATTERNS.DEAD_LOCK_THREAD.matcher(line);
                    if (!matcher.matches()) {
                        throw new ParserException("Illegal dead lock thread name");
                    }
                    JavaThread thread = new JavaThread();
                    String name = matcher.group("name");
                    threads.add(thread);
                    tCount++;

                    thread.setName(snapshot.getSymbols().add(name));
                    thread.setType(ThreadType.JAVA);
                    // wait and held info
                    input.readLine();
                    input.readLine();

                    line = input.readLine();

                } while (line.startsWith("\""));

                step();
                line = input.currentLine();

                if (!line.equals(PATTERNS.DEAD_LOCK_STACK_HEAD)) {
                    throw new ParserException("Illegal dead lock stack head");
                }
                // skip ====
                input.readLine();

                line = input.readLine();
                for (int i = 0; i < tCount; i++) {
                    matcher = PATTERNS.DEAD_LOCK_THREAD.matcher(line);
                    if (!matcher.matches()) {
                        throw new ParserException("Illegal dead lock thread name");
                    }
                    List<String> stackTraces = new ArrayList<>();
                    while (true) {
                        line = input.readLine();
                        if (line != null && !line.startsWith("\"") && !line.isBlank() && !line.startsWith("Found")) {
                            stackTraces.add(line);
                        } else {
                            Trace trace = parseStackTrace(threads.get(i), true, stackTraces);
                            threads.get(i).setTrace(snapshot.getTraces().add(trace));
                            break;
                        }
                    }
                }
                snapshot.getDeadLockThreads().add(threads);
            }

            if (dlCount > 0) {
                step();
                line = input.currentLine();
                Matcher matcher = PATTERNS.DEAD_FOUND.matcher(line);
                if (!matcher.matches()) {
                    throw new ParserException("Missing Dead lock found line");
                }
                if (Integer.parseInt(matcher.group("count")) != dlCount) {
                    throw new ParserException("Dead lock count mismatched");
                }
            }
        }

        void enroll(RawJavaThread tp) {
            processing.incrementAndGet();
            try {
                QUEUE.put(tp);
            } catch (Throwable t) {
                processing.decrementAndGet();
            }
        }

        ThreadType typeOf(String name, boolean javaThread) {
            if (javaThread) {
                if (name.startsWith("C1 CompilerThread") || name.startsWith("C2 CompilerThread")) {
                    return ThreadType.JIT;
                }
                return ThreadType.JAVA;
            }

            if (name.contains("GC") || name.contains("G1") || name.contains("CMS") ||
                name.contains("Concurrent Mark-Sweep")) {
                return ThreadType.GC;
            }

            return ThreadType.VM;
        }

        void fillThread(Thread thread, Matcher m) {
            Pool<String> symbols = snapshot.getSymbols();
            String name = m.group("name");
            thread.setName(symbols.add(name));
            thread.setType(typeOf(name, thread instanceof JavaThread));
            thread.setOsPriority(Integer.parseInt(m.group("osPriority")));
            thread.setCpu(Converter.str2TimeMillis(m.group("cpu")));
            thread.setElapsed(Converter.str2TimeMillis(m.group("elapsed")));
            thread.setTid(Long.decode(m.group("tid")));
            thread.setNid(Long.decode(m.group("nid")));
            thread.setOsThreadState(OSTreadState.getByDescription(m.group("state").trim()));
        }

        void parseThreads() throws Exception {
            String line = input.currentLine();
            do {
                while (StringUtils.isBlank(line)) {
                    line = input.readLine();
                    if (line == null) {
                        return;
                    }
                }

                if (line.startsWith("\"")) {
                    if (!line.endsWith("]")) {
                        // not a java thread
                        break;
                    }
                    RawJavaThread rjt = new RawJavaThread();
                    rjt.contents.add(line);
                    rjt.lineStart = input.lineNumber();

                    while ((line = input.readLine()) != null) {
                        if (StringUtils.isBlank(line)) {
                            continue;
                        }

                        if (line.startsWith("\"")) {
                            break;
                        }

                        if (line.startsWith(MonitorState.ELIMINATED_SCALAR_REPLACED.prefix())) {
                            // this problem is fixed by JDK-8268780(JDK 18)
                            int index = line.indexOf(")");
                            if (index > 0 && line.length() > index + 1) {
                                rjt.contents.add(line.substring(0, index + 1));
                                rjt.contents.add(line.substring(index + 1).trim());
                                continue;
                            }
                        }
                        rjt.contents.add(line);
                        rjt.lineEnd = input.lineNumber();
                    }
                    enroll(rjt);
                } else {
                    break;
                }
            } while (true);

            // other threads
            do {
                while (StringUtils.isBlank(line)) {
                    line = input.readLine();
                    if (line == null) {
                        return;
                    }
                }

                if (line.startsWith("\"")) {
                    parseByElementPattern(Element.NON_JAVA_THREAD, m -> {
                        Thread thread = new Thread();
                        fillThread(thread, m);
                        thread.setLineStart(input.lineNumber());
                        thread.setLineEnd(input.lineNumber());
                        snapshot.getNonJavaThreads().add(thread);
                    }, true);
                } else {
                    break;
                }
                // step in parseByElementPattern
            } while ((line = input.currentLine()) != null);
        }

        void done() {
            int remain = processing.decrementAndGet();
            if (remain == 0) {
                synchronized (this) {
                    this.notify();
                }
            }
        }

        void recordError(Throwable t) {
            synchronized (this) {
                errors.add(t);
                t.printStackTrace();
            }
        }

        void onParseRawThreadError(Throwable t) {
            recordError(t);
            int remain = processing.decrementAndGet();
            if (remain == 0) {
                synchronized (this) {
                    this.notify();
                }
            }
        }

        void checkLastFrameNotNull(Frame last, String line) {
            if (last == null) {
                throw new ParserException("Last frame doesn't exist: " + line);
            }
        }

        Monitor assembleMonitor(Thread thread, boolean needMap, MonitorState state, long address,
                                boolean isClass, String clazz) {
            RawMonitor rm = new RawMonitor();
            rm.setAddress(address);
            rm.setClassInstance(isClass);
            rm.setClazz(clazz);
            rm = snapshot.getRawMonitors().add(rm);
            Monitor monitor = new Monitor();
            monitor.setRawMonitor(rm);
            monitor.setState(state);
            monitor = snapshot.getMonitors().add(monitor);
            if (needMap) {
                synchronized (this) {
                    boolean shouldMap = true;
                    if (state == MonitorState.LOCKED) {
                        Map<MonitorState, List<Thread>> map = snapshot.getMonitorThreads().get(rm.getId());
                        if (map != null) {
                            for (Map.Entry<MonitorState, List<Thread>> entry : map.entrySet()) {
                                if (entry.getKey() != MonitorState.LOCKED && entry.getValue().contains(thread)) {
                                    shouldMap = false;
                                    break;
                                }
                            }
                        }
                    }
                    if (shouldMap) {
                        snapshot.getMonitorThreads()
                                .computeIfAbsent(rm.getId(), i -> new HashMap<>())
                                .computeIfAbsent(state, s -> new ArrayList<>())
                                .add(thread);
                    }
                }
            }
            return monitor;
        }

        Trace parseStackTrace(Thread thread, boolean deadLockThread, List<String> stackTraces) {
            Pool<String> symbolPool = snapshot.getSymbols();
            Pool<Frame> framePool = snapshot.getFrames();
            Pool<Monitor> monitorPool = snapshot.getMonitors();

            Trace trace = new Trace();
            List<Frame> frames = new ArrayList<>();
            List<Monitor> monitors = new ArrayList<>();
            Frame last = null;
            for (int i = 0; i < stackTraces.size(); i++) {
                Matcher m;
                String line = stackTraces.get(i);
                if (line.startsWith("at")) {
                    m = PATTERNS.JAVA_FRAME.matcher(line);
                    if (!m.matches()) {
                        throw new ParserException("Illegal java frame: " + line);
                    }

                    if (!monitors.isEmpty()) {
                        last.setMonitors(monitors.toArray(new Monitor[0]));
                        monitors.clear();
                    }

                    if (last != null) {
                        // add frame here since all related information has been processed
                        frames.add(framePool.add(last));
                    }

                    last = new Frame();
                    last.setClazz(symbolPool.add(m.group("class")));
                    last.setMethod(symbolPool.add(m.group("method")));
                    String module = m.group("module");
                    if (module != null) {
                        // strip '/'
                        last.setModule(symbolPool.add(module.substring(0, module.length() - 1)));
                    }
                    String source = m.group("source");
                    SourceType sourceType = SourceType.judge(source);
                    last.setSourceType(sourceType);
                    if (sourceType == SourceType.SOURCE_FILE_WITH_LINE_NUMBER) {
                        int index = source.indexOf(":");
                        last.setLine(Integer.parseInt(source.substring(index + 1)));
                        source = source.substring(0, index);
                        last.setSource(symbolPool.add(source));
                    } else if (sourceType == SourceType.SOURCE_FILE) {
                        last.setSource(symbolPool.add(source));
                    }

                } else {
                    if (line.startsWith(MonitorState.PARKING.prefix())) {
                        assert last != null;
                        m = PATTERNS.PARKING.matcher(line);
                        if (!m.matches()) {
                            throw new ParserException("Illegal parking line: " + line);
                        }
                        monitors.add(assembleMonitor(thread, !deadLockThread, MonitorState.PARKING,
                                                     Long.decode(m.group("address")),
                                                     false, symbolPool.add(m.group("class"))));
                    } else if (line.startsWith(MonitorState.WAITING_ON.prefix())) {
                        assert last != null;
                        if (line.contains("<no object reference available>")) {
                            monitors
                                .add(assembleMonitor(thread, !deadLockThread,
                                                     MonitorState.WAITING_ON_NO_OBJECT_REFERENCE_AVAILABLE,
                                                     -1, false, null));
                        } else {
                            m = PATTERNS.WAITING_ON.matcher(line);
                            if (!m.matches()) {
                                throw new ParserException("Illegal waiting line: " + line);
                            }
                            monitors
                                .add(assembleMonitor(thread, !deadLockThread, MonitorState.WAITING_ON,
                                                     Long.decode(m.group("address")),
                                                     m.group("isClass") != null,
                                                     symbolPool.add(m.group("class"))));
                        }
                    } else if (line.startsWith(MonitorState.WAITING_TO_RE_LOCK.prefix())) {
                        assert last != null;
                        m = PATTERNS.WAITING_TO_RE_LOCK.matcher(line);
                        if (!m.matches()) {
                            throw new ParserException("Illegal waiting to re-lock line: " + line);
                        }
                        monitors
                            .add(assembleMonitor(thread, !deadLockThread, MonitorState.WAITING_TO_RE_LOCK,
                                                 Long.decode(m.group("address")),
                                                 m.group("isClass") != null,
                                                 symbolPool.add(m.group("class"))));
                    } else if (line.startsWith(MonitorState.WAITING_ON_CLASS_INITIALIZATION.prefix())) {
                        assert last != null;
                        m = PATTERNS.WAITING_ON_CLASS_INITIALIZATION.matcher(line);
                        if (!m.matches()) {
                            throw new ParserException(
                                "Illegal waiting on class initialization line: " + line);
                        }
                        monitors
                            .add(assembleMonitor(thread, !deadLockThread,
                                                 MonitorState.WAITING_ON_CLASS_INITIALIZATION,
                                                 -1, true, symbolPool.add(m.group("class"))));
                    } else if (line.startsWith(MonitorState.LOCKED.prefix())) {
                        checkLastFrameNotNull(last, line);
                        m = PATTERNS.LOCKED.matcher(line);
                        if (!m.matches()) {
                            throw new ParserException("Illegal locked line: " + line);
                        }
                        monitors.add(assembleMonitor(thread, !deadLockThread, MonitorState.LOCKED,
                                                     Long.decode(m.group("address")),
                                                     m.group("isClass") != null,
                                                     symbolPool.add(m.group("class"))));
                    } else if (line.startsWith(MonitorState.WAITING_TO_LOCK.prefix())) {
                        checkLastFrameNotNull(last, line);
                        m = PATTERNS.WAITING_TO_LOCK.matcher(line);
                        if (!m.matches()) {
                            throw new ParserException("Illegal waiting to lock line: " + line);
                        }
                        monitors.add(assembleMonitor(thread, !deadLockThread, MonitorState.WAITING_TO_LOCK,
                                                     Long.decode(m.group("address")),
                                                     m.group("isClass") != null,
                                                     symbolPool.add(m.group("class"))));
                    } else if (line.startsWith(MonitorState.ELIMINATED.prefix())) {
                        checkLastFrameNotNull(last, line);
                        m = PATTERNS.ELIMINATED.matcher(line);
                        if (!m.matches()) {
                            throw new ParserException("Illegal eliminated lock line: " + line);
                        }
                        monitors.add(assembleMonitor(thread, !deadLockThread, MonitorState.ELIMINATED,
                                                     Long.decode(m.group("address")),
                                                     m.group("isClass") != null,
                                                     symbolPool.add(m.group("class"))));
                    } else if (line.startsWith(MonitorState.ELIMINATED_SCALAR_REPLACED.prefix())) {
                        checkLastFrameNotNull(last, line);
                        m = PATTERNS.ELIMINATED_SCALAR_REPLACED.matcher(line);
                        if (!m.matches()) {
                            throw new ParserException(
                                "Illegal eliminated(scalar replaced) lock line: " + line);
                        }
                        monitors.add(assembleMonitor(thread, !deadLockThread,
                                                     MonitorState.ELIMINATED_SCALAR_REPLACED,
                                                     -1,
                                                     false,
                                                     symbolPool.add(m.group("class"))));
                    } else if (line.equals(PATTERNS.LOCKED_OWNABLE_SYNCHRONIZERS)) {
                        // concurrent locks
                        int lockIndex = i + 1;
                        line = stackTraces.get(lockIndex);
                        if (PATTERNS.NONE.equals(line)) {
                            if (lockIndex + 1 != stackTraces.size()) {
                                throw new ParserException("Should not have content after: " + line);
                            }
                        } else {
                            Pool<ConcurrentLock> concurrentPool = snapshot.getConcurrentLocks();
                            List<ConcurrentLock> concurrentLocks = new ArrayList<>();
                            do {
                                m = PATTERNS.LOCKED_SYNCHRONIZER.matcher(line);
                                if (!m.matches()) {
                                    throw new ParserException("Illegal lock synchronizer line: " + line);
                                }
                                ConcurrentLock concurrentLock = new ConcurrentLock();
                                concurrentLock.setAddress(Long.decode(m.group("address")));
                                concurrentLock.setClazz(symbolPool.add(m.group("class")));
                                concurrentLocks.add(concurrentPool.add(concurrentLock));

                                if (++lockIndex < stackTraces.size()) {
                                    line = stackTraces.get(lockIndex);
                                } else {
                                    break;
                                }
                            } while (true);
                            trace.setConcurrentLocks(
                                concurrentLocks.toArray(new ConcurrentLock[0]));
                        }
                        break;
                    } else {
                        throw new ParserException("Unrecognized line: " + line);
                    }
                }
            }

            if (last != null) {
                if (!monitors.isEmpty()) {
                    last.setMonitors(monitors.toArray(new Monitor[0]));
                }
                frames.add(framePool.add(last));
            }

            trace.setFrames(frames.toArray(new Frame[0]));
            return trace;
        }

        void parse(RawJavaThread rjt) {
            try {
                List<String> contents = rjt.contents;
                assert contents.size() >= 2;

                String line = contents.get(0);
                Matcher m = PATTERNS.JAVA_THREAD.matcher(contents.get(0));
                if (!m.matches()) {
                    throw new ParserException("Illegal java thread: " + line);
                }
                JavaThread thread = new JavaThread();
                fillThread(thread, m);
                thread.setLineStart(rjt.lineStart);
                thread.setLineEnd(rjt.lineEnd);
                thread.setJid(Long.parseLong(m.group("id")));
                thread.setDaemon(m.group("daemon") != null);
                thread.setPriority(Integer.parseInt(m.group("priority")));
                thread.setLastJavaSP(Long.decode(m.group("lastJavaSP")));

                // java thread state
                line = contents.get(1);
                m = PATTERNS.JAVA_STATE.matcher(line);
                if (!m.matches()) {
                    throw new ParserException("Illegal java thread state: " + line);
                }
                thread.setJavaThreadState(JavaThreadState.getByDescription(m.group("state")));

                if (contents.size() > 2 && thread.getType() == ThreadType.JAVA /* skip jit */) {
                    // trace
                    Trace trace = parseStackTrace(thread, false, contents.subList(2, contents.size()));
                    snapshot.getCallSiteTree().add(trace);
                    thread.setTrace(snapshot.getTraces().add(trace));
                }
                synchronized (this) {
                    snapshot.getJavaThreads().add(thread);
                }
                done();
            } catch (Throwable t) {
                onParseRawThreadError(t);
            }
        }

        enum Element {

            TIME("dump time"),

            VERSION("vm version"),

            JNI_GLOBAL_REFS("JNI global references"),

            NON_JAVA_THREAD("Non Java Thread");

            private final String description;

            Element(String description) {
                this.description = description;
            }
        }

        interface Action {
            void onMatched(Matcher matcher) throws Exception;
        }

        class RawJavaThread {

            private final List<String> contents;

            private int lineStart;

            private int lineEnd;

            public RawJavaThread() {
                contents = new ArrayList<>();
            }

            void parse() {
                ParserImpl.this.parse(this);
            }
        }
    }
}

/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.profile.lang.java.helper;

import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.jifa.profile.lang.java.model.JavaThreadCPUTime;
import org.eclipse.jifa.profile.model.StackTrace;
import org.eclipse.jifa.profile.model.Frame;
import org.eclipse.jifa.profile.model.TaskCount;
import org.eclipse.jifa.profile.model.TaskResultBase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleFlameGraph {
    public List<SimpleFlameGraphNode> roots = new ArrayList<>();
    public BigDecimal totalSampleValue = new BigDecimal(0);

    public static SimpleFlameGraph parse(JavaThreadCPUTime tct) {
        SimpleFlameGraph g = new SimpleFlameGraph();
        long totalCpuTime = tct.totalCPUTime();
        Map<StackTrace, Long> samples = tct.getSamples();

        if (samples != null) {
            AtomicLong stackTraceCount = new AtomicLong();
            samples.values().forEach(stackTraceCount::addAndGet);
            long stackTraceCpuTime = totalCpuTime / stackTraceCount.get();

            samples.keySet().forEach(item -> {
                long count = samples.get(item);
                g.addStackTrace(item, stackTraceCpuTime * count);
            });
        }

        return g;
    }

    public static SimpleFlameGraph parse(TaskResultBase ta) {
        SimpleFlameGraph g = new SimpleFlameGraph();
        Map<StackTrace, Long> samples = ta.getSamples();
        samples.keySet().forEach(item -> {
            long count = samples.get(item);
            g.addStackTrace(item, count);
        });

        return g;
    }

    public void addStackTrace(StackTrace st, long sampleValue) {
        totalSampleValue = totalSampleValue.add(new BigDecimal(sampleValue));
        Frame[] frames = st.getFrames();
        if (frames == null || frames.length == 0) {
            return;
        }

        Frame[] reverseFrames = new Frame[frames.length];
        for (int i = 0; i < frames.length; i++) {
            reverseFrames[i] = frames[frames.length - 1 - i];
        }

        addFrames(null, reverseFrames, 0, sampleValue);
    }

    public List<Triple<String, String, String>> queryLeafNodes(double threshold) {
        return queryLeafNodes(null, threshold);
    }

    public List<Triple<String, String, String>> queryLeafNodes(String leafKw) {
        return queryLeafNodes(leafKw, 0);
    }

    public List<Triple<String, String, String>> queryLeafNodes(String leafKw, double threshold) {
        List<SimpleFlameGraphNode> list = new ArrayList<>();
        this.queryLeafNodes(null, leafKw, threshold, list);
        List<Triple<String, String, String>> result = new ArrayList<>();
        list.forEach(node -> {
            double percent = node.getValue().multiply(BigDecimal.valueOf(100))
                    .divide(totalSampleValue, 2, RoundingMode.HALF_UP).doubleValue();
            Triple<String, String, String> value =
                    Triple.of(node.getName(), String.valueOf(node.getValue().longValue()), String.valueOf(percent));
            result.add(value);
        });
        return result;
    }

    public void queryLeafNodes(SimpleFlameGraphNode currentNode, String leafKw, double threshold,
                               List<SimpleFlameGraphNode> resultList) {
        List<SimpleFlameGraphNode> children;
        if (currentNode == null) {
            children = roots;
        } else {
            children = currentNode.getChildren();
        }

        if (children == null || children.isEmpty()) {
            if (currentNode != null) {
                double percent = currentNode.getValue().multiply(BigDecimal.valueOf(100))
                        .divide(totalSampleValue, 2, RoundingMode.HALF_UP).doubleValue();
                if (percent >= threshold) {
                    if (leafKw == null || currentNode.getName().contains(leafKw)) {
                        resultList.add(currentNode);
                    }
                }
            }
        } else {
            for (SimpleFlameGraphNode child : children) {
                queryLeafNodes(child, leafKw, threshold, resultList);
            }
        }
    }

    public void clearPrintFlag(SimpleFlameGraphNode currentNode) {
        List<SimpleFlameGraphNode> children;
        if (currentNode == null) {
            children = roots;
        } else {
            children = currentNode.getChildren();
        }

        if (currentNode != null) {
            currentNode.setPrint(false);
        }

        if (children != null && !children.isEmpty()) {
            for (SimpleFlameGraphNode child : children) {
                clearPrintFlag(child);
            }
        }
    }

    public void matchAndSetPrintFlag(SimpleFlameGraphNode currentNode, String leafKw, double threshold) {
        List<SimpleFlameGraphNode> children;
        if (currentNode == null) {
            children = roots;
        } else {
            children = currentNode.getChildren();
        }

        if (children == null || children.isEmpty()) {
            if (currentNode != null) {
                if (currentNode.getValue().multiply(BigDecimal.valueOf(100))
                        .divide(totalSampleValue, RoundingMode.HALF_UP).intValue() >= threshold) {
                    if (leafKw == null || currentNode.getName().contains(leafKw)) {
                        currentNode.setPrint(true);
                    }
                }
            }
        } else {
            for (SimpleFlameGraphNode child : children) {
                matchAndSetPrintFlag(child, leafKw, threshold);
                if (child.isPrint()) {
                    if (currentNode != null) {
                        currentNode.setPrint(true);
                    }
                }
            }
        }
    }

    public String dump(double threshold) {
        return dump(null, threshold);
    }

    public String dump() {
        return dump(null);
    }

    public String dump(String leafKw) {
        return dump(leafKw, 0);
    }

    public String dump(String leafKw, double threshold) {
        StringBuffer buffer = new StringBuffer();
        dump(buffer, null, 0, leafKw, threshold);
        return buffer.toString();
    }

    public void dump(StringBuffer buffer, SimpleFlameGraphNode currentNode, int indent, String leafKw,
                     double threshold) {
        clearPrintFlag(currentNode);
        matchAndSetPrintFlag(currentNode, leafKw, threshold);

        List<SimpleFlameGraphNode> children;
        if (currentNode == null) {
            children = roots;
        } else {
            children = currentNode.getChildren();
        }

        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < Math.max(0, indent); i++) {
            padding.append(" ");
        }

        if (currentNode != null) {
            String msg = String.format("%s%s [%d] [%.2f]\n",
                    padding, currentNode.getName(), currentNode.getValue().longValue(),
                    currentNode.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(totalSampleValue, 2, RoundingMode.HALF_UP).doubleValue());
            buffer.append(msg);
        }

        if (children != null && !children.isEmpty()) {
            for (SimpleFlameGraphNode child : children) {
                if (!child.isPrint()) {
                    continue;
                }
                dump(buffer, child, indent + 1, leafKw, threshold);
            }
        }
    }

    private void addFrames(SimpleFlameGraphNode parent, Frame[] frames, int index, long sampleValue) {
        if (frames.length == 0 || index > frames.length) {
            return;
        }

        List<SimpleFlameGraphNode> children;
        if (parent == null) {
            children = roots;
        } else {
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            children = parent.getChildren();
        }

        Frame frame = frames[index];
        String desc = frame.toString();

        Optional<SimpleFlameGraphNode> result = children.stream().filter(item -> item.getName().equals(desc)).findAny();
        SimpleFlameGraphNode childrenHead;
        if (result.isPresent()) {
            childrenHead = result.get();
            childrenHead.setValue(childrenHead.getValue().add(BigDecimal.valueOf(sampleValue)));
        } else {
            childrenHead = new SimpleFlameGraphNode();
            childrenHead.setName(desc);
            childrenHead.setValue(BigDecimal.valueOf(sampleValue));
            childrenHead.setParent(parent);
            children.add(childrenHead);
        }

        if (index + 1 == frames.length) {
            return;
        }

        addFrames(childrenHead, frames, index + 1, sampleValue);
    }
}


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
package org.eclipse.jifa.jfr.util;

import org.eclipse.jifa.jfr.model.jfr.RecordedFrame;
import org.eclipse.jifa.jfr.model.jfr.RecordedMethod;
import org.eclipse.jifa.jfr.model.jfr.RecordedStackTrace;
import org.eclipse.jifa.jfr.model.JavaFrame;
import org.eclipse.jifa.jfr.model.JavaMethod;
import org.eclipse.jifa.jfr.model.symbol.SymbolBase;
import org.eclipse.jifa.jfr.model.symbol.SymbolTable;
import org.eclipse.jifa.jfr.model.Frame;
import org.eclipse.jifa.jfr.model.StackTrace;

import java.util.List;

public class StackTraceUtil {
    // FIXME: need cache
    public static StackTrace build(RecordedStackTrace stackTrace, SymbolTable<SymbolBase> symbols) {
        StackTrace result = new StackTrace();
        result.setTruncated(stackTrace.isTruncated());

        DescriptorUtil util = new DescriptorUtil();
        List<RecordedFrame> srcFrames = stackTrace.getFrames();
        Frame[] dstFrames = new Frame[srcFrames.size()];
        for (int i = 0; i < srcFrames.size(); i++) {
            RecordedFrame frame = srcFrames.get(i);
            Frame dstFrame;
            if (frame.isJavaFrame()) {
                dstFrame = new JavaFrame();
                ((JavaFrame) dstFrame).setJavaFrame(frame.isJavaFrame());
                ((JavaFrame) dstFrame).setType(JavaFrame.Type.typeOf(frame.getType()));
                ((JavaFrame) dstFrame).setBci(frame.getBytecodeIndex());
            } else {
                dstFrame = new Frame();
            }

            RecordedMethod method = frame.getMethod();
            JavaMethod dstMethod = new JavaMethod();
            dstMethod.setPackageName(method.getType().getPackageName());
            dstMethod.setType(method.getType().getName());
            dstMethod.setName(method.getName());
            dstMethod.setDescriptor(util.decodeMethodArgs(method.getDescriptor()));

            dstMethod.setModifiers(method.getModifiers());
            dstMethod.setHidden(method.isHidden());
            if (symbols.isContains(dstMethod)) {
                dstMethod = (JavaMethod) symbols.get(dstMethod);
            } else {
                symbols.put(dstMethod);
            }

            dstFrame.setMethod(dstMethod);
            dstFrame.setLine(frame.getLineNumber());
            if (symbols.isContains(dstFrame)) {
                dstFrame = (Frame) symbols.get(dstFrame);
            } else {
                symbols.put(dstFrame);
            }

            dstFrames[i] = dstFrame;
        }

        result.setFrames(dstFrames);
        if (symbols.isContains(result)) {
            result = (StackTrace) symbols.get(result);
        } else {
            symbols.put(result);
        }

        return result;
    }
}

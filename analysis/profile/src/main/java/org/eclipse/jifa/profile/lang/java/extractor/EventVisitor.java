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
package org.eclipse.jifa.profile.lang.java.extractor;

import org.eclipse.jifa.profile.lang.java.model.jfr.RecordedEvent;

public abstract class EventVisitor {
    void visitUnsignedIntFlag(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitGarbageCollection(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitCPUInformation(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitEnvVar(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitCPCRuntimeInformation(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitActiveSetting(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitThreadStart(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitProcessCPULoad(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitThreadCPULoad(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitExecutionSample(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitNativeExecutionSample(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitExecuteVMOperation(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitObjectAllocationInNewTLAB(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitObjectAllocationOutsideTLAB(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitFileRead(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitFileWrite(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitFileForce(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitSocketRead(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitSocketWrite(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitJavaMonitorEnter(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitJavaMonitorWait(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitThreadPark(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitClassLoad(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitThreadSleep(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitSocketConnect(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }
}

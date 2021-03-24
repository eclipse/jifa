/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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

import lombok.Data;
import org.eclipse.jifa.common.enums.FileTransferState;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.common.enums.ProgressState;
import org.eclipse.jifa.worker.support.FileSupport;

@Data
public class TransferListener {

    private ProgressState state;
    private long totalSize;
    private long transferredSize;
    private String errorMsg;

    private FileType fileType;
    private String fileName;

    public TransferListener(FileType fileType, String originalName, String fileName) {
        this.fileType = fileType;
        this.fileName = fileName;
        this.state = ProgressState.NOT_STARTED;
        FileSupport.initInfoFile(fileType, originalName, fileName);
    }

    public synchronized void updateState(ProgressState state) {
        if (this.state == state) {
            return;
        }

        if (state == ProgressState.SUCCESS) {
            FileSupport.updateTransferState(fileType, fileName, FileTransferState.SUCCESS);
            totalSize = FileSupport.info(fileType, fileName).getSize();
        }

        if (state == ProgressState.ERROR) {
            FileSupport.updateTransferState(fileType, fileName, FileTransferState.ERROR);
        }

        if (state == ProgressState.IN_PROGRESS && this.state == ProgressState.NOT_STARTED) {
            FileSupport.updateTransferState(fileType, fileName, FileTransferState.IN_PROGRESS);
        }

        this.state = state;
    }

    public synchronized void addTransferredSize(long bytes) {
        this.transferredSize += bytes;
    }
}

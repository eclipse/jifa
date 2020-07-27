/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.worker.vo.heapdump.overview;

import lombok.Data;

@Data
public class Details {
    private String jvmInfo;
    private int identifierSize;
    private long creationDate;
    private int numberOfObjects;
    private int numberOfGCRoots;
    private int numberOfClasses;
    private int numberOfClassLoaders;
    private long usedHeapSize;
    private boolean generationInfoAvailable;

    public Details(String jvmInfo, int identifierSize, long creationDate, int numberOfObjects, int numberOfGCRoots,
                   int numberOfClasses, int numberOfClassLoaders, long usedHeapSize, boolean generationInfoAvailable) {
        this.jvmInfo = jvmInfo;
        this.identifierSize = identifierSize;
        this.creationDate = creationDate;
        this.numberOfObjects = numberOfObjects;
        this.numberOfGCRoots = numberOfGCRoots;
        this.numberOfClasses = numberOfClasses;
        this.numberOfClassLoaders = numberOfClassLoaders;
        this.usedHeapSize = usedHeapSize;
        this.generationInfoAvailable = generationInfoAvailable;
    }
}

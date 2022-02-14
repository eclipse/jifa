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

import org.eclipse.jifa.common.listener.ProgressListener;
import org.eclipse.jifa.tda.model.Snapshot;

import java.nio.file.Path;

/**
 * Thread dump parser
 */
public interface Parser {

    /**
     * Generate a snapshot for the thread dump identified by path
     *
     * @param path     the path of thread dump
     * @param listener progress listener for parsing
     * @return the snapshot of thread dump
     * @throws ParserException the exception occurred during parsing
     */
    Snapshot parse(Path path, ProgressListener listener) throws ParserException;
}

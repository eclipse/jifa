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
package org.eclipse.jifa.profile.lang.java;

import org.eclipse.jifa.analysis.listener.ProgressListener;
import org.eclipse.jifa.profile.lang.java.model.AnalysisResult;
import org.eclipse.jifa.profile.exception.ProfileAnalysisException;
import org.eclipse.jifa.profile.lang.java.request.AnalysisRequest;

public interface JFRAnalyzer {
    AnalysisResult execute(AnalysisRequest request, ProgressListener listener) throws ProfileAnalysisException;
}
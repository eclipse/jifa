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

package org.eclipse.jifa.gclog;

import org.eclipse.jifa.common.listener.DefaultProgressListener;
import org.eclipse.jifa.gclog.model.*;
import org.eclipse.jifa.gclog.parser.GCLogParserFactory;
import org.eclipse.jifa.gclog.parser.JDK8G1GCLogParser;
import org.eclipse.jifa.gclog.vo.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.eclipse.jifa.gclog.TestUtil.stringToBufferedReader;
import static org.eclipse.jifa.gclog.model.GCEvent.UNKNOWN_INT;
import static org.eclipse.jifa.gclog.model.GCEventType.*;
import static org.eclipse.jifa.gclog.vo.HeapGeneration.*;

public class TestGCDiagnoser {
}

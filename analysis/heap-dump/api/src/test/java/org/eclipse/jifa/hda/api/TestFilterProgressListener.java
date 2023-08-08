/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.hda.api;

import org.eclipse.jifa.analysis.listener.ProgressListener;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TestFilterProgressListener {

    @Test
    public void test() {
        ProgressListener mock = Mockito.mock(ProgressListener.class);
        FilterProgressListener listener = new FilterProgressListener(mock);

        Mockito.doNothing().when(mock).beginTask(Mockito.anyString(), Mockito.anyInt());
        listener.beginTask("name", 1);
        Mockito.verify(mock, Mockito.times(1)).beginTask(Mockito.anyString(), Mockito.anyInt());

        Mockito.doNothing().when(mock).subTask(Mockito.anyString());
        listener.subTask("name");
        Mockito.verify(mock, Mockito.times(1)).subTask(Mockito.anyString());

        Mockito.doNothing().when(mock).worked(Mockito.anyInt());
        listener.worked(1);
        Mockito.verify(mock, Mockito.times(1)).worked(Mockito.anyInt());

        Mockito.doNothing().when(mock).sendUserMessage(Mockito.any(), Mockito.anyString(), Mockito.any());
        listener.sendUserMessage(ProgressListener.Level.INFO, "message", null);
        Mockito.verify(mock, Mockito.times(1)).sendUserMessage(Mockito.any(), Mockito.anyString(), Mockito.any());

        Mockito.doNothing().when(mock).reset();
        listener.reset();
        Mockito.verify(mock, Mockito.times(1)).reset();

        Mockito.doReturn("").when(mock).log();
        listener.log();
        Mockito.verify(mock, Mockito.times(1)).log();

        Mockito.doReturn(1.0d).when(mock).percent();
        listener.percent();
        Mockito.verify(mock, Mockito.times(1)).percent();
    }
}

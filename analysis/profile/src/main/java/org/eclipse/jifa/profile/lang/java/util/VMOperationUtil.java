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
package org.eclipse.jifa.profile.lang.java.util;

import org.eclipse.jifa.profile.model.Frame;
import org.eclipse.jifa.profile.model.Method;
import org.eclipse.jifa.profile.model.StackTrace;

public class VMOperationUtil {
    public static StackTrace makeStackTrace(String name) {
        StackTrace st = new StackTrace();
        Frame[] frames = new Frame[2];
        st.setFrames(frames);

        Frame f = new Frame();
        Method m = new Method();
        m.setType("JVM");
        m.setName(name);
        m.setDescriptor("");
        f.setMethod(m);
        frames[0] = f;

        f = new Frame();
        m = new Method();
        m.setType("JVM");
        m.setName("JVM");
        m.setDescriptor("");
        f.setMethod(m);
        frames[1] = f;

        return st;
    }
}

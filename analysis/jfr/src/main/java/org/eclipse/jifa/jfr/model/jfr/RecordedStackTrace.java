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
package org.eclipse.jifa.jfr.model.jfr;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.jfr.model.symbol.SymbolBase;

import java.util.List;
import java.util.Objects;

@Setter
@Getter
public class RecordedStackTrace extends SymbolBase {
    private boolean truncated;
    private List<RecordedFrame> frames;

    public boolean isEquals(Object b) {
        if (!(b instanceof RecordedStackTrace)) {
            return false;
        }

        RecordedStackTrace t2 = (RecordedStackTrace) b;

        if (truncated != t2.isTruncated()) {
            return false;
        }

        if (frames == null) {
            return t2.getFrames() == null;
        }

        if (frames.size() != t2.getFrames().size()) {
            return false;
        }

        return frames.equals(t2.getFrames());
    }

    public int genHashCode() {
        return Objects.hash(truncated, frames);
    }
}

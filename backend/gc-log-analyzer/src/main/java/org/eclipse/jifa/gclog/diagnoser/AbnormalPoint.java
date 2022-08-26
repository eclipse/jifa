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
package org.eclipse.jifa.gclog.diagnoser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jifa.gclog.event.TimedEvent;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.util.I18nStringView;

import java.util.Comparator;
import java.util.List;

import static org.eclipse.jifa.gclog.diagnoser.AbnormalSeverity.NONE;
import static org.eclipse.jifa.gclog.diagnoser.AbnormalType.LAST_TYPE;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AbnormalPoint {
    private AbnormalType type;
    private TimedEvent site;
    private AbnormalSeverity severity;

    public static final AbnormalPoint LEAST_SERIOUS = new AbnormalPoint(LAST_TYPE, null, NONE);

    public static final Comparator<AbnormalPoint> compareByImportance = (ab1, ab2) -> {
        if (ab1.severity != ab2.severity) {
            return ab1.severity.ordinal() - ab2.severity.ordinal();
        } else if (ab1.type != ab2.type) {
            return ab1.type.getOrdinal() - ab2.type.getOrdinal();
        }
        return 0;
    };

    public List<I18nStringView> generateDefaultSuggestions(GCModel model) {
        return new DefaultSuggestionGenerator(model, this).generate();
    }
}

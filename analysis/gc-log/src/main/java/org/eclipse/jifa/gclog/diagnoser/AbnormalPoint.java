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

import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jifa.gclog.event.TimedEvent;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.util.I18nStringView;

import java.util.Comparator;
import java.util.List;

import static org.eclipse.jifa.gclog.diagnoser.AbnormalType.LAST_TYPE;

@Data
public class AbnormalPoint {
    private AbnormalType type;
    private TimedEvent site;
    private List<I18nStringView> defaultSuggestions;

    public static final AbnormalPoint LEAST_SERIOUS = new AbnormalPoint(LAST_TYPE, null);

    public AbnormalPoint(AbnormalType type, TimedEvent site) {
        this.type = type;
        this.site = site;
    }

    public static final Comparator<AbnormalPoint> compareByImportance = (ab1, ab2) -> {
        if (ab1.type != ab2.type) {
            return ab1.type.getOrdinal() - ab2.type.getOrdinal();
        }
        return 0;
    };

    public void generateDefaultSuggestions(GCModel model) {
        this.defaultSuggestions = new DefaultSuggestionGenerator(model, this).generate();
    }

    public AbnormalPointVO toVO() {
        AbnormalPointVO vo = new AbnormalPointVO();
        vo.setType(type.getName());
        vo.setDefaultSuggestions(defaultSuggestions);
        return vo;
    }

    @Override
    public String toString() {
        return "AbnormalPoint{" +
                "type=" + type +
                ", defaultSuggestions=" + defaultSuggestions +
                '}';
    }

    @Data
    @NoArgsConstructor
    public static class AbnormalPointVO {
        // don't use I18nStringView because frontend need to check this field
        private String type;
        private List<I18nStringView> defaultSuggestions;
    }
}

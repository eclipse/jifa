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
package org.eclipse.jifa.server.condition;

import org.eclipse.jifa.server.enums.Role;
import org.eclipse.jifa.server.enums.SchedulingStrategy;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

class OnSchedulingStrategy implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Role role = context.getEnvironment().getRequiredProperty("jifa.role", Role.class);
        if (role != Role.MASTER) {
            return false;
        }
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnSchedulingStrategy.class.getName());
        SchedulingStrategy expected = (SchedulingStrategy) attributes.get("value");
        SchedulingStrategy actual = context.getEnvironment().getProperty("jifa.scheduling-strategy", SchedulingStrategy.class, null);
        return expected == actual;
    }
}

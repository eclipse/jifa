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
package org.eclipse.jifa.analysis;

import java.lang.reflect.Type;

/**
 * @param name                 the parameter name
 * @param type                 tpe parameter type
 * @param required             whether the parameter is required
 * @param targetPath           whether the parameter is the target path
 * @param comparisonTargetPath whether the parameter is the comparison target path
 */
public record ApiParameter(String name, Type type, boolean required, boolean targetPath, boolean comparisonTargetPath) {
}

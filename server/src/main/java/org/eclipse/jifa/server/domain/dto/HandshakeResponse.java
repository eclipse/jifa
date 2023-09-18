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
package org.eclipse.jifa.server.domain.dto;

import org.eclipse.jifa.server.enums.Role;

import java.util.Map;

public record HandshakeResponse(Role serverRole,
                                boolean allowAnonymousAccess,
                                boolean allowRegistration,
                                PublicKey publicKey,
                                Map<String, String> oauth2LoginLinks,
                                User user) {
}

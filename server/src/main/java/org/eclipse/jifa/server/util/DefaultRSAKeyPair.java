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
package org.eclipse.jifa.server.util;

import org.eclipse.jifa.common.domain.exception.CommonException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public abstract class DefaultRSAKeyPair {

    private static final KeyPair DEFAULT;

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            DEFAULT = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new CommonException(e);
        }
    }

    public static RSAPublicKey getPublicKey() {
        return (RSAPublicKey) DEFAULT.getPublic();
    }

    public static RSAPrivateKey getPrivateKey() {
        return (RSAPrivateKey) DEFAULT.getPrivate();
    }
}

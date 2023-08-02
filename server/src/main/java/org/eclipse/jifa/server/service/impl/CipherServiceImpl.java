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
package org.eclipse.jifa.server.service.impl;

import org.eclipse.jifa.common.domain.exception.CommonException;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.service.CipherService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.util.Base64;

@Service
public class CipherServiceImpl extends ConfigurationAccessor implements CipherService {

    public String encrypt(String raw) {
        try {
            Cipher cipher = Cipher.getInstance(getPublicKey().getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, getPublicKey());
            byte[] bytes = cipher.doFinal(raw.getBytes(Constant.CHARSET));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Throwable t) {
            throw new CommonException(t);
        }
    }

    @Override
    public String decrypt(String encoded) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            Cipher cipher = Cipher.getInstance(getPrivateKey().getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, getPrivateKey());
            return new String(cipher.doFinal(bytes), Constant.CHARSET);
        } catch (Throwable t) {
            throw new CommonException(t);
        }
    }

    @Override
    public String getPublicKeyString() {
        byte[] publicKeyBytes = getPublicKey().getEncoded();
        return Base64.getEncoder().encodeToString(publicKeyBytes);
    }
}

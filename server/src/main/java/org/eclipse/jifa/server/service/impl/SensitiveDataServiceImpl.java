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

import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.domain.exception.DecryptionException;
import org.eclipse.jifa.server.service.SensitiveDataService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.util.Base64;

@Service
public class SensitiveDataServiceImpl extends ConfigurationAccessor implements SensitiveDataService {

    @Override
    public String decrypt(String encodedText) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encodedText);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, getPrivateKey());
            return new String(cipher.doFinal(bytes), Constant.CHARSET);
        } catch (Throwable t) {
            throw new DecryptionException(t);
        }
    }

    @Override
    public String getPublicKeyString() {
        byte[] publicKeyBytes = getPublicKey().getEncoded();
        return Base64.getEncoder().encodeToString(publicKeyBytes);
    }
}

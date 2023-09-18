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

import jakarta.annotation.PostConstruct;
import org.eclipse.jifa.common.domain.exception.CommonException;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.domain.dto.PublicKey;
import org.eclipse.jifa.server.service.CipherService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

@Service
public class CipherServiceImpl extends ConfigurationAccessor implements CipherService {

    private PublicKey key;

    @PostConstruct
    private void init() {
        key = new PublicKey(pkcs8(), ssh2());
    }

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
    public PublicKey getPublicKeyString() {
        return key;
    }

    private String pkcs8() {
        byte[] publicKeyBytes = getPublicKey().getEncoded();
        return """
                -----BEGIN PUBLIC KEY-----
                %s
                -----END PUBLIC KEY-----
                """.formatted(Base64.getEncoder().encodeToString(publicKeyBytes));
    }

    private String ssh2() {
        String prefix = "ssh-rsa";

        RSAPublicKey key = getPublicKey();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.writeBytes(new byte[]{0, 0, 0, 7});
        bos.writeBytes(prefix.getBytes());

        byte[] exponent = key.getPublicExponent().toByteArray();
        bos.writeBytes(toLengthBytes(exponent.length));
        bos.writeBytes(exponent);

        byte[] module = key.getModulus().toByteArray();
        bos.writeBytes(toLengthBytes(module.length));
        bos.writeBytes(module);

        return prefix + " " + Base64.getEncoder().encodeToString(bos.toByteArray());
    }

    private static byte[] toLengthBytes(int length) {
        return new byte[]{(byte) (length >> 24), (byte) (length >> 16), (byte) (length >> 8), (byte) length};
    }
}

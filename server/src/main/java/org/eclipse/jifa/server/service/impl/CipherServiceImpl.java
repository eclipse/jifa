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
import org.eclipse.jifa.common.util.Validate;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.domain.dto.PublicKey;
import org.eclipse.jifa.server.domain.entity.shared.ConfigurationEntity;
import org.eclipse.jifa.server.repository.ConfigurationRepo;
import org.eclipse.jifa.server.service.CipherService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

@Service
public class CipherServiceImpl extends ConfigurationAccessor implements CipherService {

    private final ConfigurationRepo configurationRepo;

    private final TransactionTemplate transactionTemplate;

    private RSAPublicKey publicKey;

    private RSAPrivateKey privateKey;

    private PublicKey key;

    public CipherServiceImpl(ConfigurationRepo configurationRepo, TransactionTemplate transactionTemplate) {
        this.configurationRepo = configurationRepo;
        this.transactionTemplate = transactionTemplate;
    }

    @PostConstruct
    private void init() throws NoSuchAlgorithmException, InvalidKeySpecException {
        Optional<ConfigurationEntity> optional = configurationRepo.findByUniqueName(Constant.CONFIGURATION_PUBLIC_KEY);
        if (optional.isEmpty()) {
            KeyPair keyPair;
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
            RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
            String rsaPublicKeyString = Base64.getEncoder().encodeToString(rsaPublicKey.getEncoded());
            RSAPrivateKey rasPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
            String rsaPrivateKeyString = Base64.getEncoder().encodeToString(rasPrivateKey.getEncoded());
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    ConfigurationEntity publicKeyEntity = new ConfigurationEntity();
                    publicKeyEntity.setUniqueName(Constant.CONFIGURATION_PUBLIC_KEY);
                    publicKeyEntity.setContent(rsaPublicKeyString);
                    ConfigurationEntity privateKeyEntity = new ConfigurationEntity();
                    privateKeyEntity.setUniqueName(Constant.CONFIGURATION_PRIVATE_KEY);
                    privateKeyEntity.setContent(rsaPrivateKeyString);
                    configurationRepo.save(publicKeyEntity);
                    configurationRepo.save(privateKeyEntity);
                });
                publicKey = rsaPublicKey;
                privateKey = rasPrivateKey;
            } catch (DataIntegrityViolationException ignored) {
                // find again
                optional = configurationRepo.findByUniqueName(Constant.CONFIGURATION_PUBLIC_KEY);
                Validate.isTrue(optional.isPresent(), "Failed to init key");
            }
        }

        if (optional.isPresent()) {
            publicKey = deserializePublicKey(optional.get().getContent());
            optional = configurationRepo.findByUniqueName(Constant.CONFIGURATION_PRIVATE_KEY);
            Validate.isTrue(optional.isPresent(), "Failed to init key");
            assert optional.isPresent();
            privateKey = deserializePrivateKey(optional.get().getContent());
        }

        key = new PublicKey(pkcs8(), ssh2());
    }

    @Override
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
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public PublicKey getPublicKeyString() {
        return key;
    }

    private String pkcs8() {
        byte[] publicKeyBytes = publicKey.getEncoded();
        return """
                -----BEGIN PUBLIC KEY-----
                %s
                -----END PUBLIC KEY-----
                """.formatted(Base64.getEncoder().encodeToString(publicKeyBytes));
    }

    private String ssh2() {
        String prefix = "ssh-rsa";

        RSAPublicKey key = publicKey;
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

    private byte[] toLengthBytes(int length) {
        return new byte[]{(byte) (length >> 24), (byte) (length >> 16), (byte) (length >> 8), (byte) length};
    }

    private RSAPublicKey deserializePublicKey(String content) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encoded = Base64.getDecoder().decode(content);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
    }

    private RSAPrivateKey deserializePrivateKey(String content) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encoded = Base64.getDecoder().decode(content);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }
}

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

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.model.DownloadFileRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import jakarta.annotation.PostConstruct;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.TransferListener;
import net.schmizz.sshj.xfer.scp.SCPDownloadClient;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;
import org.apache.commons.io.FileUtils;
import org.eclipse.jifa.common.util.ExecutorFactory;
import org.eclipse.jifa.common.util.Validate;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.condition.ElasticSchedulingStrategy;
import org.eclipse.jifa.server.condition.Master;
import org.eclipse.jifa.server.domain.dto.FileTransferRequest;
import org.eclipse.jifa.server.enums.FileType;
import org.eclipse.jifa.server.service.StorageService;
import org.eclipse.jifa.server.support.FileTransferListener;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

@Conditional(StorageServiceImpl.Accessible.class)
@Service
public class StorageServiceImpl extends ConfigurationAccessor implements StorageService {

    static class Accessible extends AnyNestedCondition {

        public Accessible() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @SuppressWarnings("unused")
        @Master
        @ElasticSchedulingStrategy
        static class MasterWithElasticSchedulingStrategy {
        }

        @org.eclipse.jifa.server.condition.Worker
        static class Worker {

        }
    }

    private Executor executor;

    private Path basePath;

    @PostConstruct
    private void init() {
        basePath = config.getStoragePath();
        Validate.isTrue(Files.isDirectory(basePath));

        executor = ExecutorFactory.newExecutor("File Transfer");
    }

    @Override
    public long getAvailableSpace() throws IOException {
        return Files.getFileStore(basePath).getUsableSpace();
    }

    @Override
    public long getTotalSpace() throws IOException {
        return Files.getFileStore(basePath).getTotalSpace();
    }

    @Override
    public void handleTransfer(FileTransferRequest request, String destFilename, FileTransferListener listener) {
        Path destination = provision(request.getType(), destFilename);
        executor.execute(() -> {
            boolean success = false;
            try {
                listener.onStart();
                switch (request.getMethod()) {
                    case OSS -> handleOSS(destination, listener, request.getOss());
                    case S3 -> handleS3(destination, listener, request.getS3());
                    case SCP -> handleSCP(destination, listener, request.getScp());
                    case URL -> handleURL(destination, listener, request.getUrl());
                }
                success = true;
            } catch (Throwable t) {
                try {
                    listener.onError(t);
                } finally {
                    scavenge(request.getType(), destFilename);
                }
            }
            if (success) {
                listener.onSuccess(destination.toFile().length());
            }
        });
    }

    @Override
    public long handleUpload(FileType type, MultipartFile file, String destFilename) throws Throwable {
        Path destination = provision(type, destFilename);
        try {
            file.transferTo(destination);
            return destination.toFile().length();
        } catch (Throwable t) {
            scavenge(type, destFilename);
            throw t;
        }
    }

    @Override
    public Path locationOf(FileType type, String name) {
        return basePath.resolve(type.getStorageDirectoryName()).resolve(name).resolve(name);
    }

    @Override
    public void scavenge(FileType type, String name) {
        Path directory = basePath.resolve(type.getStorageDirectoryName()).resolve(name);
        FileUtils.deleteQuietly(directory.toFile());
    }

    @Override
    public Map<FileType, Set<String>> getAllFiles() {
        Map<FileType, Set<String>> map = new HashMap<>();
        for (FileType type : FileType.values()) {
            Path directory = basePath.resolve(type.getStorageDirectoryName());
            if (Files.exists(directory)) {
                String[] names = directory.toFile().list();
                if (names != null) {
                    map.put(type, Set.of(names));
                }
            }
        }
        return map;
    }

    private Path provision(FileType type, String name) {
        Path directory = basePath.resolve(type.getStorageDirectoryName()).resolve(name);
        Validate.isTrue(directory.toFile().mkdirs());
        return directory.resolve(name);
    }

    private void handleOSS(Path destination, FileTransferListener listener, FileTransferRequest.OSS oss) throws Throwable {
        OSSClient ossClient = new OSSClient(oss.getEndpoint(),
                                            new DefaultCredentialProvider(oss.getAccessKeyId(), oss.getAccessKeySecret()),
                                            null);
        try {
            ObjectMetadata meta = ossClient.getObjectMetadata(oss.getBucketName(), oss.getObjectName());
            listener.fireTotalSize(meta.getContentLength());

            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(oss.getBucketName(), oss.getObjectName());
            downloadFileRequest.setDownloadFile(destination.toFile().getAbsolutePath());
            downloadFileRequest.setPartSize(128 * 1024 * 1024);
            downloadFileRequest.setTaskNum(Runtime.getRuntime().availableProcessors());
            downloadFileRequest.setEnableCheckpoint(true);

            AtomicLong transferredSize = new AtomicLong(0);
            downloadFileRequest.withProgressListener(progressEvent -> {
                ProgressEventType eventType = progressEvent.getEventType();
                if (eventType == ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT) {
                    listener.fireTransferredSize(transferredSize.addAndGet(progressEvent.getBytes()));
                }
            });
            ossClient.downloadFile(downloadFileRequest);
        } finally {
            ossClient.shutdown();
        }
    }

    private void handleS3(Path destination, FileTransferListener listener, FileTransferRequest.S3 s3) {
        AWSCredentials credentials = new BasicAWSCredentials(s3.getAccessKey(), s3.getSecretKey());
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTPS);
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                                                 .withCredentials(new AWSStaticCredentialsProvider(credentials))
                                                 .withCredentials(new InstanceProfileCredentialsProvider(false))
                                                 .withClientConfiguration(clientConfig)
                                                 .withRegion(s3.getRegion())
                                                 .withPathStyleAccessEnabled(true)
                                                 .build();
        try {
            com.amazonaws.services.s3.model.ObjectMetadata objectMetadata =
                    s3Client.getObjectMetadata(s3.getBucketName(), s3.getObjectName());
            listener.fireTotalSize(objectMetadata.getContentLength());

            AtomicLong transferredSize = new AtomicLong(0);
            GetObjectRequest getObjectRequest = new GetObjectRequest(s3.getBucketName(), s3.getObjectName())
                    .withGeneralProgressListener(progressEvent -> {
                        if (progressEvent.getEventType() == com.amazonaws.event.ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT) {
                            listener.fireTransferredSize(transferredSize.addAndGet(progressEvent.getBytes()));
                        }
                    });
            s3Client.getObject(getObjectRequest, destination.toFile());
        } finally {
            s3Client.shutdown();
        }
    }

    private void handleSCP(Path destination, FileTransferListener listener, FileTransferRequest.SCP scp) throws IOException {
        try (SSHClient ssh = new SSHClient()) {
            ssh.connect(scp.getHostname());
            if (scp.getPassword() != null) {
                ssh.authPassword(scp.getUser(), scp.getPassword());
            } else {
                // TODO
                ssh.authPublickey(scp.getUser(), (KeyProvider) null);
            }

            SCPFileTransfer transfer = ssh.newSCPFileTransfer();
            transfer.setTransferListener(new TransferListener() {
                @Override
                public TransferListener directory(String name) {
                    return this;
                }

                @Override
                public StreamCopier.Listener file(String name, long size) {
                    listener.fireTotalSize(size);
                    return listener::fireTransferredSize;
                }
            });
            SCPDownloadClient downloadClient = transfer.newSCPDownloadClient();
            // do not copy dir now
            downloadClient.setRecursiveMode(false);
            downloadClient.copy(scp.getPath(), new FileSystemFile(destination.toFile().getAbsolutePath()));
        }
    }

    private void handleURL(Path destination, FileTransferListener listener, String url) throws IOException {
        URLConnection conn = new java.net.URL(url).openConnection();
        listener.fireTotalSize(Math.max(conn.getContentLengthLong(), 0));
        try (InputStream in = conn.getInputStream();
             OutputStream out = new FileOutputStream(destination.toFile())) {
            byte[] buffer = new byte[8192];
            int length;
            int transferredSize = 0;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
                transferredSize += length;
                listener.fireTransferredSize(transferredSize);
            }
        }
    }
}

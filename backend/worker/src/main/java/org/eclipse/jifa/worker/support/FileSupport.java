/********************************************************************************
 * Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.worker.support;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.model.DownloadFileRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3Object;


import io.vertx.core.Promise;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.scp.SCPDownloadClient;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;
import org.apache.commons.io.FileUtils;
import org.eclipse.jifa.common.Constant;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.common.enums.FileTransferState;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.common.enums.ProgressState;
import org.eclipse.jifa.common.util.FileUtil;
import org.eclipse.jifa.common.vo.FileInfo;
import org.eclipse.jifa.common.vo.TransferringFile;
import org.eclipse.jifa.worker.WorkerGlobal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;
import static org.eclipse.jifa.common.util.GsonHolder.GSON;
import static org.eclipse.jifa.worker.Constant.File.INFO_FILE_SUFFIX;

public class FileSupport {

    public static final List<String> PUB_KEYS = new ArrayList<>();

    private static final String ERROR_LOG = "error.log";

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSupport.class);

    private static final Map<String, TransferListener> transferListeners = new ConcurrentHashMap<>();

    private static String[] keyLocations() {
        final String base = System.getProperty("user.home") + File.separator + ".ssh" + File.separator;
        return new String[]{base + "jifa-ssh-key"};
    }

    private static String[] pubKeyLocations() {
        final String base = System.getProperty("user.home") + File.separator + ".ssh" + File.separator;
        return new String[]{base + "jifa-ssh-key.pub"};
    }

    public static void init() {
        for (FileType type : FileType.values()) {
            File file = new File(WorkerGlobal.workspace() + File.separator + type.getTag());
            if (file.exists()) {
                ASSERT.isTrue(file.isDirectory(), String.format("%s must be directory", file.getAbsolutePath()));
            } else {
                ASSERT.isTrue(file.mkdirs(), String.format("Can not create %s ", file.getAbsolutePath()));
            }
        }

        for (String loc : pubKeyLocations()) {
            File f = new File(loc);
            if (f.exists() && f.length() > 0) {
                PUB_KEYS.add(FileUtil.content(f));
            }
        }
    }

    public static void initInfoFile(FileType type, String originalName, String name) {
        ASSERT.isTrue(new File(dirPath(type, name)).mkdirs(), "Make directory failed");

        FileInfo info = buildInitFileInfo(type, originalName, name);
        try {
            FileUtils.write(infoFile(type, name), GSON.toJson(info), Charset.defaultCharset());
        } catch (IOException e) {
            LOGGER.error("Write file information failed", e);
            throw new JifaException(e);
        }
    }

    private static FileInfo buildInitFileInfo(FileType type, String originalName, String name) {
        FileInfo info = new FileInfo();
        info.setOriginalName(originalName);
        info.setName(name);
        info.setSize(0);
        info.setType(type);
        info.setTransferState(FileTransferState.NOT_STARTED);
        info.setDownloadable(false);
        info.setCreationTime(System.currentTimeMillis());
        return info;
    }

    public static List<FileInfo> info(FileType type) {
        List<FileInfo> infoList = new ArrayList<>();
        File dir = new File(dirPath(type));
        ASSERT.isTrue(dir.isDirectory(), ErrorCode.SANITY_CHECK);

        File[] subDirs = dir.listFiles(File::isDirectory);
        if (subDirs == null) {
            return infoList;
        }

        for (File subDir : subDirs) {
            String infoFileName = subDir.getName() + INFO_FILE_SUFFIX;
            File[] files = subDir.listFiles((d, name) -> infoFileName.equals(name));
            if (files != null && files.length == 1) {
                File infoFile = files[0];
                try {
                    FileInfo info = GSON.fromJson(FileUtils.readFileToString(infoFile, Charset.defaultCharset()),
                                                  FileInfo.class);
                    ensureValidFileInfo(info);
                    infoList.add(info);
                } catch (Exception e) {
                    LOGGER.error("Read file information failed: {}", infoFile.getAbsolutePath(), e);
                    // should not throw exception here
                }
            }
        }
        return infoList;
    }

    private static void ensureValidFileInfo(FileInfo info) {
        ASSERT.notNull(info)
              .notNull(info.getOriginalName())
              .notNull(info.getName())
              .notNull(info.getType())
              .notNull(info.getTransferState())
              .isTrue(info.getSize() >= 0)
              .isTrue(info.getCreationTime() > 0);
    }

    public static FileInfo getOrGenInfo(FileType type, String name) {
        File file = new File(FileSupport.filePath(type, name));
        ASSERT.isTrue(file.exists(), ErrorCode.FILE_DOES_NOT_EXIST);

        File infoFile = infoFile(type, name);
        if (infoFile.exists()) {
            return info(type, name);
        }

        FileInfo fileInfo = buildInitFileInfo(type, name, name);
        fileInfo.setCreationTime(file.lastModified());
        fileInfo.setTransferState(FileTransferState.SUCCESS);
        fileInfo.setSize(file.length());
        save(fileInfo);
        return fileInfo;
    }

    public static FileInfo info(FileType type, String name) {
        File infoFile = infoFile(type, name);
        FileInfo fileInfo;
        try {
            fileInfo = GSON.fromJson(FileUtils.readFileToString(infoFile, Charset.defaultCharset()), FileInfo.class);
            ensureValidFileInfo(fileInfo);
        } catch (IOException e) {
            LOGGER.error("Read file information failed", e);
            throw new JifaException(e);
        }
        return fileInfo;
    }

    public static FileInfo infoOrNull(FileType type, String name) {
        try {
            return info(type, name);
        } catch (Exception e) {
            return null;
        }
    }

    public static void save(FileInfo info) {
        try {
            FileUtils
                .write(infoFile(info.getType(), info.getName()), GSON.toJson(info), Charset.defaultCharset());
        } catch (IOException e) {
            LOGGER.error("Save file information failed", e);
            throw new JifaException(e);
        }
    }

    public static void delete(FileType type, String name) {
        try {
            FileUtils.deleteDirectory(new File(dirPath(type, name)));
        } catch (IOException e) {
            LOGGER.error("Delete file failed", e);
            throw new JifaException(e);
        }
    }

    public static void delete(FileInfo[] fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            try {
                delete(fileInfo.getType(), fileInfo.getName());
            } catch (Throwable t) {
                LOGGER.error("Delete file failed", t);
            }
        }
    }

    public static void sync(FileInfo[] fileInfos, boolean cleanStale) {
        Map<FileType, List<String>> files = new HashMap<>(){{
            for (FileType ft : FileType.values()) {
                // In case no files returned
                this.put(ft, new ArrayList<>());
            }
        }};

        for (FileInfo fi : fileInfos) {
            files.get(fi.getType()).add(fi.getName());
        }

        long lastModified = System.currentTimeMillis() - Constant.STALE_THRESHOLD;
        for (FileType ft : files.keySet()) {
            List<String> names = files.get(ft);
            File[] listFiles = new File(dirPath(ft)).listFiles();
            if (listFiles == null) {
                continue;
            }
            for (File lf : listFiles) {
                if (names.contains(lf.getName())) {
                    continue;
                }
                LOGGER.info("{} is not synchronized", lf.getName());
                if (cleanStale && lf.lastModified() < lastModified) {
                    LOGGER.info("Delete stale file {}", lf.getName());
                    delete(ft, lf.getName());
                }
            }
        }
    }

    public static void updateTransferState(FileType type, String name, FileTransferState state) {
        FileInfo info = info(type, name);
        info.setTransferState(state);
        if (state == FileTransferState.SUCCESS) {
            // for worker, file is downloadable after transferred
            info.setSize(new File(FileSupport.filePath(type, name)).length());
            info.setDownloadable(true);
        }
        save(info);
    }

    private static String dirPath(FileType type) {
        return WorkerGlobal.workspace() + File.separator + type.getTag();
    }

    public static String dirPath(FileType type, String name) {
        String defaultDirPath = dirPath(type) + File.separator + name;
        return WorkerGlobal.hooks().mapDirPath(type, name, defaultDirPath);
    }

    private static String infoFilePath(FileType type, String name) {
        return dirPath(type, name) + File.separator + name + INFO_FILE_SUFFIX;
    }

    private static File infoFile(FileType type, String name) {
        return new File(infoFilePath(type, name));
    }

    public static String filePath(FileType type, String name) {
        return filePath(type, name, name);
    }

    public static String filePath(FileType type, String name, String childrenName) {
        String defaultFilePath = dirPath(type, name) + File.separator + childrenName;
        return WorkerGlobal.hooks().mapFilePath(type, name, childrenName, defaultFilePath);
    }

    public static String errorLogPath(FileType fileType, String file) {
        return FileSupport.filePath(fileType, file, ERROR_LOG);
    }

    public static String indexPath(FileType fileType, String file) {
        String indexFileNamePrefix;
        int i = file.lastIndexOf('.');
        if (i >= 0) {
            indexFileNamePrefix = file.substring(0, i + 1);
        } else {
            indexFileNamePrefix = file + '.';
        }
        String defaultIndexPath = FileSupport.filePath(fileType, file, indexFileNamePrefix + "index");
        return WorkerGlobal.hooks().mapIndexPath(fileType, file, defaultIndexPath);
    }

    public static TransferListener createTransferListener(FileType fileType, String originalName, String fileName) {
        TransferListener listener = new TransferListener(fileType, originalName, fileName);
        transferListeners.put(fileName, listener);
        return listener;
    }

    public static void removeTransferListener(String fileName) {
        transferListeners.remove(fileName);
    }

    public static TransferListener getTransferListener(String fileName) {
        return transferListeners.get(fileName);
    }

    public static void transferBySCP(String user, String hostname, String src, FileType fileType, String fileName,
                                     TransferListener transferProgressListener, Promise<TransferringFile> promise) {
        transferBySCP(user, null, hostname, src, fileType, fileName, transferProgressListener, promise);
    }

    public static void transferBySCP(String user, String pwd, String hostname, String src, FileType fileType,
                                     String fileName, TransferListener transferProgressListener,
                                     Promise<TransferringFile> promise) {
        transferProgressListener.updateState(ProgressState.IN_PROGRESS);
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier((h, port, key) -> true);
        try {
            ssh.connect(hostname);
            if (pwd != null) {
                ssh.authPassword(user, pwd);
            } else {
                ssh.authPublickey(user, keyLocations());
            }
            SCPFileTransfer transfer = ssh.newSCPFileTransfer();
            transfer.setTransferListener(new net.schmizz.sshj.xfer.TransferListener() {
                @Override
                public net.schmizz.sshj.xfer.TransferListener directory(String name) {
                    return this;
                }

                @Override
                public StreamCopier.Listener file(String name, long size) {
                    transferProgressListener.setTotalSize(size);
                    return transferProgressListener::setTransferredSize;
                }
            });
            SCPDownloadClient downloadClient = transfer.newSCPDownloadClient();
            promise.complete(new TransferringFile(fileName));
            // do not copy dir now
            downloadClient.setRecursiveMode(false);
            downloadClient.copy(src, new FileSystemFile(FileSupport.filePath(fileType, fileName)));
            transferProgressListener.updateState(ProgressState.SUCCESS);
        } catch (Exception e) {
            LOGGER.error("SSH transfer failed");
            handleTransferError(fileName, transferProgressListener, promise, e);
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException e) {
                LOGGER.error("SSH disconnect failed", e);
            }
        }
    }

    public static void transferByURL(String url, FileType fileType, String fileName, TransferListener listener,
                                     Promise<TransferringFile> promise) {
        InputStream in = null;
        OutputStream out = null;
        String filePath = FileSupport.filePath(fileType, fileName);
        try {
            URLConnection conn = new URL(url).openConnection();
            listener.updateState(ProgressState.IN_PROGRESS);
            promise.complete(new TransferringFile(fileName));
            listener.setTotalSize(Math.max(conn.getContentLength(), 0));
            in = conn.getInputStream();
            out = new FileOutputStream(filePath);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
                listener.addTransferredSize(length);
            }
            listener.updateState(ProgressState.SUCCESS);
        } catch (Exception e) {
            LOGGER.error("URL transfer failed");
            handleTransferError(fileName, listener, promise, e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                LOGGER.error("Close stream failed", e);
            }
        }
    }

    public static void transferByOSS(String endpoint, String accessKeyId, String accessKeySecret, String bucketName,
                                     String objectName, FileType fileType, String fileName,
                                     TransferListener transferProgressListener,
                                     Promise<TransferringFile> promise) {
        OSSClient ossClient = null;
        try {
            ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);

            ObjectMetadata meta = ossClient.getObjectMetadata(bucketName, objectName);
            transferProgressListener.setTotalSize(meta.getContentLength());
            promise.complete(new TransferringFile(fileName));

            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, objectName);
            downloadFileRequest.setDownloadFile(new File(FileSupport.filePath(fileType, fileName)).getAbsolutePath());
            // 128m per thread now
            downloadFileRequest.setPartSize(128 * 1024 * 1024);
            downloadFileRequest.setTaskNum(Runtime.getRuntime().availableProcessors());
            downloadFileRequest.setEnableCheckpoint(true);
            downloadFileRequest.withProgressListener(progressEvent -> {
                long bytes = progressEvent.getBytes();
                ProgressEventType eventType = progressEvent.getEventType();
                switch (eventType) {
                    case TRANSFER_STARTED_EVENT:
                        transferProgressListener.updateState(ProgressState.IN_PROGRESS);
                        break;
                    case RESPONSE_BYTE_TRANSFER_EVENT:
                        transferProgressListener.addTransferredSize(bytes);
                        break;
                    case TRANSFER_FAILED_EVENT:
                        transferProgressListener.updateState(ProgressState.ERROR);
                        break;
                    default:
                        break;
                }
            });
            ossClient.downloadFile(downloadFileRequest);
            transferProgressListener.updateState(ProgressState.SUCCESS);
        } catch (Throwable t) {
            LOGGER.error("OSS transfer failed");
            handleTransferError(fileName, transferProgressListener, promise, t);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public static void transferByS3(String region, String accessKey, String secretKey, String bucketName,
                                    String objectName, FileType fileType, String fileName,
                                    TransferListener transferProgressListener,
                                    Promise<TransferringFile> promise) {
        AmazonS3 s3Client = null;
        try {
            AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            ClientConfiguration clientConfig = new ClientConfiguration();
            clientConfig.setProtocol(Protocol.HTTPS);
            s3Client = AmazonS3ClientBuilder.standard()
                                            .withCredentials(new AWSStaticCredentialsProvider(credentials))
                                            .withCredentials(new InstanceProfileCredentialsProvider(false))
                                            .withClientConfiguration(clientConfig)
                                            .withRegion(region)
                                            .withPathStyleAccessEnabled(true)
                                            .build();

            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, objectName)
                .withGeneralProgressListener(progressEvent -> {
                    long bytes = progressEvent.getBytes();
                    switch (progressEvent.getEventType()) {
                        case TRANSFER_STARTED_EVENT:
                            transferProgressListener.updateState(ProgressState.IN_PROGRESS);
                            break;
                        case RESPONSE_BYTE_TRANSFER_EVENT:
                            transferProgressListener.addTransferredSize(bytes);
                            break;
                        case TRANSFER_FAILED_EVENT:
                            transferProgressListener.updateState(ProgressState.ERROR);
                            break;
                        default:
                            break;
                    }
                });

            com.amazonaws.services.s3.model.ObjectMetadata objectMetadata =
                s3Client.getObjectMetadata(bucketName, objectName);
            transferProgressListener.setTotalSize(objectMetadata.getContentLength());
            promise.complete(new TransferringFile(fileName));
            s3Client.getObject(getObjectRequest, new File(FileSupport.filePath(fileType, fileName)));
            transferProgressListener.updateState(ProgressState.SUCCESS);
        } catch (Throwable t) {
            LOGGER.error("S3 transfer failed");
            handleTransferError(fileName, transferProgressListener, promise, t);
        } finally {
            if (s3Client != null) {
                s3Client.shutdown();
            }
        }
    }

    private static void handleTransferError(String fileName, TransferListener transferProgressListener,
                                            Promise<TransferringFile> promise, Throwable t) {
        if (promise.future().isComplete()) {
            transferProgressListener.updateState(ProgressState.ERROR);
            Throwable cause = t;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            transferProgressListener.setErrorMsg(cause.toString());
        } else {
            FileSupport.delete(transferProgressListener.getFileType(), fileName);
            removeTransferListener(fileName);
        }
        throw new JifaException(ErrorCode.TRANSFER_ERROR, t);
    }

    public static long getTotalDiskSpace() {
        return new File(System.getProperty("user.home")).getTotalSpace() >> 20;
    }

    public static long getUsedDiskSpace() {
        return FileUtils.sizeOfDirectory(new File(System.getProperty("user.home"))) >> 20;
    }

}

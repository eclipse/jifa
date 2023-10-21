/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.tda.parser;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.analysis.listener.ProgressListener;
import org.eclipse.jifa.tda.model.Snapshot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class SerDesParser implements Parser {

    private static final ThreadLocal<Kryo> KRYO;

    static {
        KRYO = ThreadLocal.withInitial(() -> {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            return kryo;
        });
    }

    private final Parser parser;

    public SerDesParser(Parser parser) {
        this.parser = parser;
    }

    @Override
    public Snapshot parse(Path path, ProgressListener listener) {
        // TODO: multi-threads support
        Path serializedDataPath = resolveSerializedDataPath(path);
        if (Files.exists(serializedDataPath)) {
            try {
                listener.beginTask("Deserializing thread dump", 100);
                Snapshot snapshot = deserialize(serializedDataPath);
                listener.worked(100);
                return snapshot;
            } catch (Throwable t) {
                log.error("Failed to deserialize thread dump: {}", t.getMessage());
                listener.sendUserMessage(ProgressListener.Level.WARNING, "Deserialize thread dump failed", t);
                listener.reset();
            }
        }

        Snapshot snapshot = parser.parse(path, listener);
        try {
            listener.beginTask("Serializing thread dump", 5);
            serialize(snapshot, serializedDataPath);
        } catch (Throwable t) {
            log.warn("Failed to serialize thread dump: {}", t.getMessage());
        } finally {
            listener.worked(5);
        }
        return snapshot;
    }

    private Path resolveSerializedDataPath(Path source) {
        return Paths.get(source.toFile().getAbsoluteFile() + ".kryo");
    }

    private void serialize(Snapshot snapshot, Path path) throws FileNotFoundException {
        Kryo kryo = KRYO.get();
        try (Output out = new Output(new FileOutputStream(path.toFile()))) {
            kryo.writeObject(out, snapshot);
        }
    }

    private Snapshot deserialize(Path path) throws IOException {
        Kryo kryo = KRYO.get();
        try (Input input = new Input(new FileInputStream(path.toFile()))) {
            return kryo.readObject(input, Snapshot.class);
        }
    }
}

/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.worker.route;

import com.google.gson.reflect.TypeToken;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import org.eclipse.jifa.common.enums.ProgressState;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.common.vo.Progress;
import org.eclipse.jifa.common.vo.Result;
import org.eclipse.jifa.hda.api.Model;
import org.eclipse.jifa.worker.WorkerGlobal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.jifa.common.Constant.HTTP_GET_OK_STATUS_CODE;
import static org.eclipse.jifa.common.Constant.HTTP_POST_CREATED_STATUS_CODE;
import static org.eclipse.jifa.common.util.GsonHolder.GSON;

public class HeapDumpRouteSuite extends Base {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeapDumpRouteSuite.class);

    static TestContext context;

    public static void test(TestContext c) throws Exception {
        context = c;
        Holder holder = new Holder();

        testGet("/isFirstAnalysis",
                (PostProcessor) resp -> {
                    Type type = new TypeToken<Result<Boolean>>() {
                    }.getType();
                    Result<Boolean> result = GSON.fromJson(resp.bodyAsString(), type);
                    context.assertTrue(result.getResult(), resp.bodyAsString());

                });

        testPost("/analyze");

        AtomicBoolean success = new AtomicBoolean();
        while (!success.get()) {
            testGet("/progressOfAnalysis",
                    (PostProcessor) resp -> {
                        Progress progress = GSON.fromJson(resp.bodyAsString(), Progress.class);
                        ProgressState state = progress.getState();
                        context.assertTrue(state == ProgressState.IN_PROGRESS || state == ProgressState.SUCCESS,
                                resp.bodyAsString());
                        if (state == ProgressState.SUCCESS) {
                            success.set(true);
                        }
                    });
            Thread.sleep(200);
        }

        // overview
        testGet("/details");
        testGet("/biggestObjects");

        // class loader
        testGet("/classLoaderExplorer/summary");
        testGet("/classLoaderExplorer/classLoader",
                req -> req.addQueryParam("page", "1")
                        .addQueryParam("pageSize", "10"),
                resp -> {
                    Type type = new TypeToken<PageView<Model.ClassLoader.Item>>() {
                    }.getType();
                    PageView<Model.ClassLoader.Item> result = GSON.fromJson(resp.bodyAsString(), type);
                    holder.id = result.getData().get(0).getObjectId();
                });
        testGet("/classLoaderExplorer/children",
                (PreProcessor) req -> req.addQueryParam("classLoaderId", String.valueOf(holder.id))
                        .addQueryParam("page", "1")
                        .addQueryParam("pageSize", "10"));

        // class reference
        testGet("/classReference/inbounds/class",
                (PreProcessor) req -> req.addQueryParam("objectId", String.valueOf(holder.id)));
        testGet("/classReference/outbounds/class",
                (PreProcessor) req -> req.addQueryParam("objectId", String.valueOf(holder.id)));

        // direct byte buffer
        testGet("/directByteBuffer/summary");
        testGet("/directByteBuffer/records",
                (PreProcessor) req -> req.addQueryParam("page", "1")
                        .addQueryParam("pageSize", "10"));

        // dominator tree
        testGet("/dominatorTree/roots",
                (PreProcessor) req -> req.addQueryParam("page", "1")
                        .addQueryParam("pageSize", "10")
                        .addQueryParam("sortBy", "id")
                        .addQueryParam("ascendingOrder", "true")
                        .addQueryParam("grouping", "NONE"));
        testGet("/dominatorTree/children",
                (PreProcessor) req -> req.addQueryParam("page", "1")
                        .addQueryParam("sortBy", "id")
                        .addQueryParam("ascendingOrder", "true")
                        .addQueryParam("pageSize", "10")
                        .addQueryParam("grouping", "NONE")
                        .addQueryParam("idPathInResultTree", "[" + holder.id + "]")
                        .addQueryParam("parentObjectId", String.valueOf(holder.id)));

        // gc root
        testGet("/GCRoots");
        testGet("/GCRoots/classes",
                (PreProcessor) req -> req.addQueryParam("page", "1")
                        .addQueryParam("pageSize", "10")
                        .addQueryParam("rootTypeIndex", "1"));
        testGet("/GCRoots/class/objects",
                (PreProcessor) req -> req.addQueryParam("page", "1")
                        .addQueryParam("pageSize", "10")
                        .addQueryParam("rootTypeIndex", "1")
                        .addQueryParam("classIndex", "1"));

        // histogram
        testGet("/histogram",
                (PreProcessor) req -> req.addQueryParam("page", "1")
                        .addQueryParam("pageSize", "10")
                        .addQueryParam("sortBy", "id")
                        .addQueryParam("ascendingOrder", "true")
                        .addQueryParam("groupingBy", "BY_CLASS"));

        // inspector
        testGet("/inspector/objectView",
                req -> req.addQueryParam("objectId", String.valueOf(holder.id)),
                resp -> {
                    Model.InspectorView view = GSON.fromJson(resp.bodyAsString(), Model.InspectorView.class);
                    holder.objectAddress = view.getObjectAddress();
                });
        testGet("/inspector/addressToId",
                (PreProcessor) req -> req.addQueryParam("objectAddress", String.valueOf(holder.objectAddress)));
        testGet("/inspector/value",
                (PreProcessor) req -> req.addQueryParam("objectId", String.valueOf(holder.id)));
        testGet("/inspector/fields",
                (PreProcessor) req -> req.addQueryParam("objectId", String.valueOf(holder.id))
                        .addQueryParam("page", "1")
                        .addQueryParam("pageSize", "10"));
        testGet("/inspector/staticFields",
                (PreProcessor) req -> req.addQueryParam("objectId", String.valueOf(holder.id))
                        .addQueryParam("page", "1")
                        .addQueryParam("pageSize", "10"));

        // leak report
        testGet("/leak/report");

        // object list
        testGet("/outbounds",
                (PreProcessor) req -> req.addQueryParam("objectId", String.valueOf(holder.id))
                        .addQueryParam("page", "1")
                        .addQueryParam("pageSize", "10"));
        testGet("/inbounds",
                (PreProcessor) req -> req.addQueryParam("objectId", String.valueOf(holder.id))
                        .addQueryParam("page", "1")
                        .addQueryParam("pageSize", "10"));

        // object
        testGet("/object",
                (PreProcessor) req -> req.addQueryParam("objectId", String.valueOf(holder.id)));

        // oql
        testGet("/oql",
                (PreProcessor) req -> req.addQueryParam("oql", "select * from java.lang.String")
                        .addQueryParam("page", "1")
                        .addQueryParam("sortBy", "id")
                        .addQueryParam("ascendingOrder", "true")
                        .addQueryParam("pageSize", "10"));

        // oql
        testGet("/sql",
                (PreProcessor) req -> req.addQueryParam("sql", "select * from java.lang.String")
                        .addQueryParam("page", "1")
                        .addQueryParam("sortBy", "id")
                        .addQueryParam("ascendingOrder", "true")
                        .addQueryParam("pageSize", "10"));

        // path to gc roots
        testGet("/pathToGCRoots",
                (PreProcessor) req -> req.addQueryParam("origin", String.valueOf(holder.id))
                        .addQueryParam("skip", "0")
                        .addQueryParam("count", "10"));

        // system property
        testGet("/systemProperties");

        // thread
        testGet("/threadsSummary");
        testGet("/threads",
                req -> req.addQueryParam("page", "1")
                        .addQueryParam("sortBy", "id")
                        .addQueryParam("ascendingOrder", "true")
                        .addQueryParam("pageSize", "10"),
                resp -> {
                    Type type = new TypeToken<PageView<Model.Thread.Item>>() {
                    }.getType();
                    PageView<Model.Thread.Item> result = GSON.fromJson(resp.bodyAsString(), type);
                    holder.id = result.getData().get(0).getObjectId();
                }
        );
        testGet("/stackTrace",
                (PreProcessor) req -> req.addQueryParam("objectId", String.valueOf(holder.id)));
        testGet("/locals",
                (PreProcessor) req -> req.addQueryParam("objectId", String.valueOf(holder.id))
                        .addQueryParam("depth", "1")
                        .addQueryParam("firstNonNativeFrame", "false"));


        // unreachable objects
        testGet("/unreachableObjects/summary");
        testGet("/unreachableObjects/records",
                (PreProcessor) req -> req.addQueryParam("page", "1")
                        .addQueryParam("pageSize", "10"));
    }

    static void testGet(String uri) {
        testGet(uri, null, null);
    }

    static void testGet(String uri, PreProcessor processor) {
        testGet(uri, processor, null);
    }

    static void testGet(String uri, PostProcessor postProcessor) {
        testGet(uri, null, postProcessor);
    }

    static void testGet(String uri, PreProcessor processor, PostProcessor postProcessor) {
        test(uri, HttpMethod.GET, processor, postProcessor);
    }

    static void testPost(String uri) {
        test(uri, HttpMethod.POST, null, null);
    }

    static void test(String uri, HttpMethod method, PreProcessor processor, PostProcessor postProcessor) {
        LOGGER.info("test {}", uri);
        Async async = context.async();
        LOGGER.info("method = {}, port = {}, host = {}, uri = {}", method, WorkerGlobal.PORT, WorkerGlobal.HOST,
                uri("/heap-dump/" + TEST_HEAP_DUMP_FILENAME + uri));
        HttpRequest<Buffer> request =
                CLIENT.request(method, WorkerGlobal.PORT, WorkerGlobal.HOST,
                        uri("/heap-dump/" + TEST_HEAP_DUMP_FILENAME + uri));
        if (processor != null) {
            processor.process(request);
        }
        request.send(
                ar -> {
                    context.assertTrue(ar.succeeded(), ar.cause() != null ? ar.cause().getMessage() : "");
                    LOGGER.debug("{}: {} - {}", uri, ar.result().statusCode(), ar.result().bodyAsString());
                    context.assertEquals(ar.result().statusCode(),
                            method == HttpMethod.GET ? HTTP_GET_OK_STATUS_CODE : HTTP_POST_CREATED_STATUS_CODE,
                            ar.result().bodyAsString());

                    if (postProcessor != null) {
                        postProcessor.process(ar.result());
                    }
                    LOGGER.info("{}: {}", uri, ar.result().bodyAsString());
                    async.complete();
                }
        );
        async.awaitSuccess();
    }

    interface PreProcessor {
        void process(HttpRequest<Buffer> request);
    }

    interface PostProcessor {
        void process(HttpResponse<Buffer> resp);
    }

    static class Holder {
        int id;

        long objectAddress;
    }
}

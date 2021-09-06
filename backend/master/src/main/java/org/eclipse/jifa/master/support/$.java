/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.master.support;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Asynchronous programming utilities. The goal of providing these utilities
 * is coding like synchronous paradigm, running asynchronously
 * <p>
 * {code async} would run target function specified by argument and transform
 * result of target function to an awaitable {code Future}. This {code Future}
 * can be used as argument of {code await}. This means, you can use {code async}
 * directly if you do not care about its result. But you <strong>MUST</strong>
 * pass a {code Future} that produced by {code async} when you call {code await}.
 * Any other Future are consider illegal and will <strong>potentially block</strong>
 * current thread.
 */
public class $ {
    public static <T> T await(Future<T> future) {
        synchronized (future) {
            try {
                while (!future.isComplete()) {
                    future.wait(TimeUnit.SECONDS.toMillis(10));
                }
            } catch (Throwable e) {
                System.out.println("Possible timeout or interrupted");
                e.printStackTrace();
            }
        }
        if (future.succeeded()) {
            return future.result();
        } else {
            throw new RuntimeException(future.cause());
        }
    }

    // Variant 1:
    // Accept functions that returning non-void value, we will ignore this return value, through.
    public static <P1, P2, P3, R> Future<R> async(ForthFunction<P1, P2, P3, Handler<AsyncResult<R>>, ?> func, P1 param1, P2 param2, P3 param3) {
        Promise<R> promise = Promise.promise();
        func.apply(param1, param2, param3, event -> {
            promise.handle(event);
            synchronized (promise.future()) {
                promise.future().notify();
            }
        });
        return promise.future();
    }

    public static void async(String methodName, Object... args) {

    }

    public static <P1, P2, R> Future<R> async(TriFunction<P1, P2, Handler<AsyncResult<R>>, ?> func, P1 param1, P2 param2) {
        Promise<R> promise = Promise.promise();
        func.apply(param1, param2, event -> {
            promise.handle(event);
            synchronized (promise.future()) {
                promise.future().notify();
            }
        });
        return promise.future();
    }

    public static <P1, R> Future<R> async(BiFunction<P1, Handler<AsyncResult<R>>, ?> func, P1 param1) {
        Promise<R> promise = Promise.promise();
        func.apply(param1, event -> {
            promise.handle(event);
            synchronized (promise.future()) {
                promise.future().notify();
            }
        });
        return promise.future();
    }


    public static <R> Future<R> async(Function<Handler<AsyncResult<R>>, ?> func) {
        Promise<R> promise = Promise.promise();
        func.apply(event -> {
            promise.handle(event);
            synchronized (promise.future()) {
                promise.future().notify();
            }
        });
        return promise.future();
    }

    // Variant 2:
    // Accept functions that returning void

    public static <P1, P2, P3, R> Future<R> asyncVoid(ForthConsumer<P1, P2, P3, Handler<AsyncResult<R>>> func, P1 param1, P2 param2, P3 param3) {
        Promise<R> promise = Promise.promise();
        func.accpet(param1, param2, param3, event -> {
            promise.handle(event);
            synchronized (promise.future()) {
                promise.future().notify();
            }
        });
        return promise.future();
    }

    public static <P1, P2, R> Future<R> asyncVoid(TriConsumer<P1, P2, Handler<AsyncResult<R>>> func, P1 param1, P2 param2) {
        Promise<R> promise = Promise.promise();
        func.accept(param1, param2, event -> {
            promise.handle(event);
            synchronized (promise.future()) {
                promise.future().notify();
            }
        });
        return promise.future();
    }

    public static <P1, R> Future<R> asyncVoid(BiConsumer<P1, Handler<AsyncResult<R>>> func, P1 param1) {
        Promise<R> promise = Promise.promise();
        func.accept(param1, event -> {
            promise.handle(event);
            synchronized (promise.future()) {
                promise.future().notify();
            }
        });
        return promise.future();
    }


    public static <R> Future<R> asyncVoid(Consumer<Handler<AsyncResult<R>>> func) {
        Promise<R> promise = Promise.promise();
        func.accept(event -> {
            promise.handle(event);
            synchronized (promise.future()) {
                promise.future().notify();
            }
        });
        return promise.future();
    }
}

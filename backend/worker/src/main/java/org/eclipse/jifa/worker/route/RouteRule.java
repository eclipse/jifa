package org.eclipse.jifa.worker.route;

import io.vertx.ext.web.RoutingContext;
import org.eclipse.jifa.common.request.AnalysisParmPack;
import org.eclipse.jifa.worker.route.HttpMethod;

import java.util.function.BiFunction;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

public class RouteRule {
    private String uri;
    private BiFunction<RoutingContext, AnalysisParmPack, Object> handleMethod;
    private HttpMethod protocal;
    private String contentType;

    public RouteRule(Object... args) {
        ASSERT.isTrue(args.length >= 2, "sanity check");
        ASSERT.isTrue(args[0] instanceof String, "sanity check");
        ASSERT.isTrue(args[1] instanceof BiFunction, "sanity check");
        this.uri = (String) args[0];
        this.handleMethod = (BiFunction<RoutingContext, AnalysisParmPack, Object>) args[1];
        if (args[2] != null) {
            intExtra((Object[]) args[2]);
        }
    }

    private void intExtra(Object[] extra) {
        if (extra.length >= 1 && extra[0] != null) {
            protocal = (HttpMethod) extra[0];
        }
        if (extra.length >= 2 && extra[1] != null) {
            contentType = (String) extra[1];
        }
    }

    public String getUri() {
        return uri;
    }

    public BiFunction<RoutingContext, AnalysisParmPack, Object> getHandleMethod() {
        return handleMethod;
    }

    public HttpMethod getProtocal() {
        return protocal;
    }

    public String getContentType() {
        return contentType;
    }
}

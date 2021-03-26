package org.eclipse.jifa.worker.route;

import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.jifa.common.aux.ErrorCode;
import org.eclipse.jifa.common.request.AnalysisParmPack;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.request.SearchAware;
import org.eclipse.jifa.common.request.SortAware;
import org.eclipse.jifa.common.util.HTTPRespGuarder;
import org.eclipse.jifa.common.vo.support.SearchType;
import org.eclipse.jifa.worker.support.hda.AnalysisEnv;

import java.util.function.BiFunction;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

public class RouteMappingTable {
    static final RouteRule[] rules = {
            // * mandatory, otherwise optional
            //
            // <request-uri>*       request uri
            // <handle-method>*     method reference that will accepts request and returns result
            // <protocol>           e.g. HttpMethod.POST
            // <content-type>       e.g. application/json; charset=utf-8
            DEF("/foo/bar", AnalysisEnv.HEAP_DUMP_ANALYZER::foo, HttpMethod.GET),
    };


    public static void register(Router router) {
        for (RouteRule rule : rules) {
            Route route = router.route(rule.getProtocal() != null ? rule.getProtocal().toVertx() : HttpMethod.GET.toVertx(), rule.getUri());
            System.out.println(route);

            route.blockingHandler(rc -> {
                try {
                    // Add content type if it is presented
                    if (rule.getContentType() != null) {
                        rc.response().putHeader("content-type", String.join(";"));
                    }
                    // Prepare parameter pack
                    AnalysisParmPack parmPack = prepareParmPack(rc.request());
                    // Call handle method
                    Object result = rule.getHandleMethod().apply(rc, parmPack);
                    // Pass result of method call to promise
                    letsPromise(rc, result);
                } catch (Throwable t) {
                    HTTPRespGuarder.fail(rc, t);
                }
            }, false);
        }
    }

    private static AnalysisParmPack prepareParmPack(HttpServerRequest request) throws IllegalAccessException {
        AnalysisParmPack pack = new AnalysisParmPack();
        // paging
        String paramPage = request.getParam("page");
        String paramPageSize = request.getParam("pageSize");
        if (paramPage != null && paramPageSize != null) {
            int page = Integer.parseInt(paramPage);
            int pageSize = Integer.parseInt(paramPageSize);
            ASSERT.isTrue(page >= 1 && pageSize >= 1, ErrorCode.ILLEGAL_ARGUMENT,
                    "must greater than 1");
            pack.setPaging(new PagingRequest(page, pageSize));

        }
        // sort
        String paramSortBy = request.getParam("sortBy");
        String paramAscendingOrder = request.getParam("ascendingOrder");
        if (paramSortBy != null && paramAscendingOrder != null) {
            String sortBy = paramSortBy;
            boolean ascendingOrder = Boolean.parseBoolean(paramAscendingOrder);
            pack.setSort(new SortAware(sortBy, ascendingOrder));
        }
        // search
        String paramSearchText = request.getParam("searchText");
        String paramSearchType = request.getParam("searchType");
        if (paramSearchText != null && paramSearchType != null) {
            String searchText = paramSearchText;
            SearchType searchType = SearchType.valueOf(paramSearchType);
            pack.setSearch(new SearchAware(searchText, searchType));
        }

        return pack;
    }

    private static void letsPromise(RoutingContext rc, Object result) {
        Promise<Object> p = Promise.promise();
        p.future().onComplete(
                event -> {
                    if (event.succeeded()) {
                        HTTPRespGuarder.ok(rc, event.result());
                    } else {
                        HTTPRespGuarder.fail(rc, event.cause());
                    }
                }
        );
        p.complete(result);
    }

    private static RouteRule DEF(String uri, BiFunction<RoutingContext, AnalysisParmPack, Object> handleMethod, Object... args) {
        RouteRule rule = new RouteRule(uri, handleMethod, args);
        return rule;
    }
}

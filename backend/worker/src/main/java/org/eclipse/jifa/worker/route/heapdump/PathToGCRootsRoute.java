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
package org.eclipse.jifa.worker.route.heapdump;

import io.vertx.core.Future;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.heapdump.HeapObject;
import org.eclipse.jifa.worker.vo.heapdump.gcrootpath.Result;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.IPathsFromGCRootsComputer;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;

import java.util.*;
import java.util.regex.Pattern;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class PathToGCRootsRoute extends HeapBaseRoute {
    private List<String> excludes =
        Arrays.asList("java.lang.ref.WeakReference:referent", "java.lang.ref.SoftReference:referent");

    private static Map<IClass, Set<String>> convert(ISnapshot snapshot,
                                                    List<String> excludes) throws SnapshotException {
        Map<IClass, Set<String>> excludeMap = null;

        if (excludes != null && !excludes.isEmpty()) {
            excludeMap = new HashMap<IClass, Set<String>>();

            for (String entry : excludes) {
                String pattern = entry;
                Set<String> fields = null;
                int colon = entry.indexOf(':');

                if (colon >= 0) {
                    fields = new HashSet<String>();

                    StringTokenizer tokens = new StringTokenizer(entry.substring(colon + 1), ","); //$NON-NLS-1$
                    while (tokens.hasMoreTokens())
                        fields.add(tokens.nextToken());

                    pattern = pattern.substring(0, colon);
                }

                for (IClass clazz : snapshot.getClassesByName(Pattern.compile(pattern), true))
                    excludeMap.put(clazz, fields);
            }
        }

        return excludeMap;
    }

    @RouteMeta(path = "/pathToGCRoots")
    void path(Future<Result> future, @ParamKey("file") String file, @ParamKey("origin") int origin,
              @ParamKey("skip") int skip, @ParamKey("count") int count) throws Exception {

        ASSERT.isTrue(origin >= 0).isTrue(skip >= 0).isTrue(count > 0);
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        Map<IClass, Set<String>> excludeMap = convert(snapshot, excludes);

        IPathsFromGCRootsComputer computer = snapshot.getPathsFromGCRoots(origin, excludeMap);

        List<int[]> paths = new ArrayList<>();
        int index = 0;
        int[] current;
        int get = 0;
        while (get < count && (current = computer.getNextShortestPath()) != null) {
            if (index < skip) {
                index++;
                continue;
            }
            paths.add(current);
            get++;
        }
        future.complete(build(snapshot, origin, paths, computer.getNextShortestPath() != null));
    }

    private Result build(ISnapshot snapshot, int originId, List<int[]> paths, boolean hasMore) throws Exception {

        Result result = new Result();
        result.setCount(paths.size());
        result.setHasMore(hasMore);
        Result.Node origin = new Result.Node();
        IObject object = snapshot.getObject(originId);
        origin.setOrigin(true);
        origin.setObjectId(originId);
        origin.setLabel(object.getDisplayName());
        origin.setSuffix(HeapDumpSupport.suffix(snapshot, originId));
        origin.setGCRoot(snapshot.isGCRoot(originId));
        origin.setObjectType(HeapObject.Type.typeOf(object));
        origin.setShallowSize(object.getUsedHeapSize());
        origin.setRetainedSize(object.getRetainedHeapSize());
        result.setTree(origin);

        if (paths.size() == 0) {
            return result;
        }

        for (int[] path : paths) {
            Result.Node parentNode = origin;
            for (int index = 1; index < path.length; index++) {
                int childId = path[index];
                Result.Node childNode = parentNode.getChild(childId);
                if (childNode == null) {
                    IObject childObj = snapshot.getObject(childId);
                    childNode = new Result.Node();
                    childNode.setObjectId(childId);
                    childNode.setPrefix(HeapDumpSupport.prefix(snapshot, childId, parentNode.getObjectId()));
                    childNode.setLabel(childObj.getDisplayName());
                    childNode.setSuffix(HeapDumpSupport.suffix(snapshot, childId));
                    childNode.setGCRoot(snapshot.isGCRoot(childId));
                    childNode.setObjectType(HeapObject.Type.typeOf(childObj));
                    childNode.setShallowSize(childObj.getUsedHeapSize());
                    childNode.setRetainedSize(childObj.getRetainedHeapSize());
                    parentNode.addChild(childNode);
                }
                parentNode = childNode;
            }
        }
        return result;
    }
}

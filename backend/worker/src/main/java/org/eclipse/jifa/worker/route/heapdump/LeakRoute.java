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

import com.google.common.base.Strings;
import io.vertx.core.Future;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.support.heapdump.SnapshotContext;
import org.eclipse.jifa.worker.vo.heapdump.HeapObject;
import org.eclipse.jifa.worker.vo.heapdump.leak.Report;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.internal.Messages;
import org.eclipse.mat.internal.snapshot.SnapshotQueryContext;
import org.eclipse.mat.internal.snapshot.inspections.Path2GCRootsQuery;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.IResultPie;
import org.eclipse.mat.query.refined.RefinedResultBuilder;
import org.eclipse.mat.query.refined.RefinedTree;
import org.eclipse.mat.query.results.CompositeResult;
import org.eclipse.mat.query.results.TextResult;
import org.eclipse.mat.report.QuerySpec;
import org.eclipse.mat.report.SectionSpec;
import org.eclipse.mat.report.Spec;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;

import java.util.ArrayList;
import java.util.List;

class LeakRoute extends HeapBaseRoute {

    private static final String OVERVIEW_PATTERN = Messages.LeakHunterQuery_Overview;

    private static final String PROBLEM_SUSPECT_PATTERN =
        Messages.LeakHunterQuery_ProblemSuspect.substring(0, Messages.LeakHunterQuery_ProblemSuspect.indexOf("{0}"));

    private static final String HIND_PATTERN =
        Messages.LeakHunterQuery_Hint.substring(0, Messages.LeakHunterQuery_Hint.indexOf("{0}"));

    private static final String DESC_PATTERN = Messages.LeakHunterQuery_Description;

    private static final String SHORTEST_PATHS_PATTERN = Messages.LeakHunterQuery_ShortestPaths;

    @RouteMeta(path = "/leak/report")
    void report(Future<Report> future, @ParamKey("file") String file) throws Exception {
        SnapshotContext context = Analyzer.getOrOpenSnapshotContext(file);
        IResult result = context.leakReport().getResult();
        Report report = new Report();
        if (result instanceof TextResult) {
            report.setInfo(((TextResult) result).getText());
        } else if (result instanceof SectionSpec) {
            report.setUseful(true);
            SectionSpec sectionSpec = (SectionSpec) result;
            report.setName(sectionSpec.getName());
            List<Spec> specs = sectionSpec.getChildren();
            for (int i = 0; i < specs.size(); i++) {
                QuerySpec spec = (QuerySpec) specs.get(i);
                String name = spec.getName();
                if (Strings.isNullOrEmpty(name)) {
                    continue;
                }
                if (name.startsWith(OVERVIEW_PATTERN)) {
                    IResultPie irtPie = (IResultPie) spec.getResult();
                    List<? extends IResultPie.Slice> pieSlices = irtPie.getSlices();

                    List<Report.Slice> slices = new ArrayList<>();
                    for (IResultPie.Slice slice : pieSlices) {
                        slices.add(new Report.Slice(slice.getLabel(), HeapDumpSupport.fetchObjectId(slice.getContext()),
                                                    slice.getValue(), slice.getDescription()));
                    }
                    report.setSlices(slices);

                } else if (name.startsWith(PROBLEM_SUSPECT_PATTERN) || name.startsWith(HIND_PATTERN)) {
                    Report.Record suspect = new Report.Record();
                    suspect.setIndex(i);
                    suspect.setName(name);
                    CompositeResult cr = (CompositeResult) spec.getResult();
                    List<CompositeResult.Entry> entries = cr.getResultEntries();
                    for (CompositeResult.Entry entry : entries) {
                        String entryName = entry.getName();
                        if (Strings.isNullOrEmpty(entryName)) {
                            IResult r = entry.getResult();
                            if (r instanceof QuerySpec && ((QuerySpec) r).getName().equals(SHORTEST_PATHS_PATTERN)) {
                                Path2GCRootsQuery.Tree tree = (Path2GCRootsQuery.Tree) ((QuerySpec) r).getResult();
                                RefinedResultBuilder builder = new RefinedResultBuilder(
                                    new SnapshotQueryContext(context.getSnapshot()), tree);
                                RefinedTree rst = (RefinedTree) builder.build();
                                List<?> elements = rst.getElements();
                                List<Report.ShortestPath> paths = new ArrayList<>();
                                suspect.setPaths(paths);
                                for (Object row : elements) {
                                    paths.add(buildPath(context.getSnapshot(), rst, row));
                                }
                            }
                        } else if ((entryName.startsWith(DESC_PATTERN) || entryName.startsWith(OVERVIEW_PATTERN))) {
                            TextResult desText = (TextResult) entry.getResult();
                            suspect.setDesc(desText.getText());
                        }
                    }
                    List<Report.Record> records = report.getRecords();
                    if (records == null) {
                        report.setRecords(records = new ArrayList<>());
                    }
                    records.add(suspect);
                }
            }
        }
        future.complete(report);
    }

    private Report.ShortestPath buildPath(ISnapshot snapshot, RefinedTree rst, Object row) throws SnapshotException {
        Report.ShortestPath shortestPath = new Report.ShortestPath();
        shortestPath.setLabel((String) rst.getColumnValue(row, 0));
        shortestPath.setShallowSize(((Bytes) rst.getColumnValue(row, 1)).getValue());
        shortestPath.setRetainedSize(((Bytes) rst.getColumnValue(row, 2)).getValue());
        int objectId = rst.getContext(row).getObjectId();
        shortestPath.setObjectId(objectId);
        IObject object = snapshot.getObject(objectId);
        shortestPath.setGCRoot(snapshot.isGCRoot(objectId));
        shortestPath.setObjectType(HeapObject.Type.typeOf(object));

        if (rst.hasChildren(row)) {
            List<Report.ShortestPath> children = new ArrayList<>();
            shortestPath.setChildren(children);
            for (Object c : rst.getChildren(row)) {
                children.add(buildPath(snapshot, rst, c));
            }
        }
        return shortestPath;
    }
}

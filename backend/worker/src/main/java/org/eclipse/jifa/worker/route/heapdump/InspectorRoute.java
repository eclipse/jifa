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
import org.eclipse.jifa.common.aux.JifaException;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.worker.route.PageViewBuilder;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.route.MappingPrefix;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.vo.heapdump.HeapObject;
import org.eclipse.jifa.worker.vo.heapdump.inspector.FieldView;
import org.eclipse.jifa.worker.vo.heapdump.inspector.ObjectView;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.*;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jifa.common.Constant.EMPTY_STRING;

@MappingPrefix("/inspector")
class InspectorRoute extends HeapBaseRoute {

    @RouteMeta(path = "/addressToId")
    void addressToId(Future<Integer> future, @ParamKey("file") String file,
                     @ParamKey("objectAddress") long address) throws SnapshotException {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        future.complete(snapshot.mapAddressToId(address));
    }

    @RouteMeta(path = "/value")
    void value(Future<String> future, @ParamKey("file") String file,
               @ParamKey("objectId") int objectId) throws SnapshotException {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        IObject object = snapshot.getObject(objectId);

        String txt = object.getClassSpecificName();

        future.complete(txt != null ? txt : EMPTY_STRING);
    }

    @RouteMeta(path = "/objectView")
    void objectView(Future<ObjectView> future, @ParamKey("file") String file,
                    @ParamKey("objectId") int objectId) throws SnapshotException {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        IObject object = snapshot.getObject(objectId);
        ObjectView view = new ObjectView();

        // object address and name
        // for non-class object, name = the name of object's class
        // for class object, name = class name
        view.setObjectAddress(object.getObjectAddress());
        IClass iClass = object instanceof IClass ? (IClass) object : object.getClazz();
        view.setName(iClass.getName());
        view.setObjectType(HeapObject.Type.typeOf(object));
        view.setGCRoot(snapshot.isGCRoot(objectId));

        // class name and address of the object
        IClass clazz = object.getClazz();
        view.setClassLabel(clazz.getTechnicalName());
        view.setClassGCRoot(clazz.getGCRootInfo() != null);

        // super class name
        if (iClass.getSuperClass() != null) {
            view.setSuperClassName(iClass.getSuperClass().getName());
        }

        // class loader name and address
        IObject classLoader = snapshot.getObject(iClass.getClassLoaderId());
        view.setClassLoaderLabel(classLoader.getTechnicalName());
        view.setClassLoaderGCRoot(classLoader.getGCRootInfo() != null);

        view.setShallowSize(object.getUsedHeapSize());
        view.setRetainedSize(object.getRetainedHeapSize());
        // gc root
        GCRootInfo[] gcRootInfo = object.getGCRootInfo();
        view.setGcRootInfo(
            gcRootInfo != null ? "GC root: " + GCRootInfo.getTypeSetAsString(object.getGCRootInfo()) : "no GC root");

        HeapLayout heapLayout = snapshot.getSnapshotInfo().getHeapLayout();
        view.setLocationType(ObjectView.locationTypeOf(heapLayout.genOf(view.getObjectAddress())));
        future.complete(view);
    }

    @RouteMeta(path = "/staticFields")
    void staticFields(Future<PageView<FieldView>> future, @ParamKey("file") String file,
                      @ParamKey("objectId") int objectId, PagingRequest pagingRequest) throws SnapshotException {

        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        IObject object = snapshot.getObject(objectId);
        boolean isClass = object instanceof IClass;
        IClass clazz = isClass ? (IClass) object : object.getClazz();

        List<Field> fields = new ArrayList<>();

        do {
            List<Field> staticFields = clazz.getStaticFields();
            for (Field staticField : staticFields) {
                if (!staticField.getName().startsWith("<")) {
                    fields.add(staticField);
                }
            }
        } while (!isClass && (clazz = clazz.getSuperClass()) != null);

        complete(future, pagingRequest, fields);
    }

    @RouteMeta(path = "/fields")
    void fields(Future<PageView<FieldView>> future, @ParamKey("file") String file, @ParamKey("objectId") int objectId,
                PagingRequest pagingRequest) throws SnapshotException {

        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        IObject object = snapshot.getObject(objectId);

        if (object instanceof IPrimitiveArray) {
            List<FieldView> fvs = new ArrayList<>();
            IPrimitiveArray pa = (IPrimitiveArray) object;
            int firstIndex = (pagingRequest.getPage() - 1) * pagingRequest.getPageSize();
            int lastIndex = Math.min(firstIndex + pagingRequest.getPageSize(), pa.getLength());
            for (int i = firstIndex; i < lastIndex; i++) {
                fvs.add(new FieldView(pa.getType(), "[" + i + "]", pa.getValueAt(i).toString()));
            }
            future.complete(new PageView<>(pagingRequest, pa.getLength(), fvs));
            return;
        } else if (object instanceof IObjectArray) {
            List<FieldView> fvs = new ArrayList<>();
            IObjectArray oa = (IObjectArray) object;
            int firstIndex = (pagingRequest.getPage() - 1) * pagingRequest.getPageSize();
            int lastIndex = Math.min(firstIndex + pagingRequest.getPageSize(), oa.getLength());
            for (int i = firstIndex; i < lastIndex; i++) {
                long refs[] = oa.getReferenceArray(i, 1);
                int refObjectId = 0;
                if (refs[0] != 0) {
                    refObjectId = snapshot.mapAddressToId(refs[0]);
                }
                String value = null;
                if (refObjectId != 0) {
                    value = getObjectValue(snapshot.getObject(refObjectId));
                }
                fvs.add(new FieldView(IObject.Type.OBJECT, "[" + i + "]", value, refObjectId));
            }
            future.complete(new PageView<>(pagingRequest, oa.getLength(), fvs));
            return;
        }

        List<Field> fields = new ArrayList<>();
        boolean isClass = object instanceof IClass;
        IClass clazz = isClass ? (IClass) object : object.getClazz();

        if (object instanceof IInstance) {
            fields.addAll(((IInstance) object).getFields());
        } else if (object instanceof IClass) {
            do {
                List<Field> staticFields = clazz.getStaticFields();
                for (Field staticField : staticFields) {
                    if (staticField.getName().startsWith("<")) {
                        fields.add(staticField);
                    }
                }
            } while ((clazz = clazz.getSuperClass()) != null);

        }
        complete(future, pagingRequest, fields);
    }

    private void complete(Future<PageView<FieldView>> future, PagingRequest pagingRequest, List<Field> totalFields) {
        future.complete(PageViewBuilder.build(totalFields, pagingRequest, field -> {
            FieldView fv = new FieldView();
            fv.setFieldType(field.getType());
            fv.setName(field.getName());
            Object value = field.getValue();
            if (value instanceof ObjectReference) {
                try {
                    fv.setObjectId(((ObjectReference) value).getObjectId());
                    fv.setValue(getObjectValue(((ObjectReference) value).getObject()));
                } catch (SnapshotException e) {
                    throw new JifaException(e);
                }
            } else if (value != null) {
                fv.setValue(value.toString());
            }
            return fv;
        }));
    }

    private String getObjectValue(IObject o) {
        String text = o.getClassSpecificName();
        return text != null ? text : o.getTechnicalName();
    }

}

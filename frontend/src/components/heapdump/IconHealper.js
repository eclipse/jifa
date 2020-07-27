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
export const ICONS = {
  id: require('../../assets/heap/id.gif'),
  size: require('../../assets/heap/size.gif'),
  roots: require('../../assets/heap/roots.gif'),
  misc: {
    sumIcon: require('../../assets/heap/misc/sum.gif'),
    sumPlusIcon: require('../../assets/heap/misc/sum_plus.gif'),
  },
  decorations: {
    gc_root: require('../../assets/heap/decorations/gc_root.gif')
  },
  objects: {
    class_obj: require('../../assets/heap/objects/class_obj.gif'),
    class_obj_gc_root: require('../../assets/heap/objects/class_obj_gc_root.gif'),
    classloader_obj: require('../../assets/heap/objects/classloader_obj.gif'),
    classloader_obj_gc_root: require('../../assets/heap/objects/classloader_obj_gc_root.gif'),
    array_obj: require('../../assets/heap/objects/array_obj.gif'),
    array_obj_gc_root: require('../../assets/heap/objects/array_obj_gc_root.gif'),
    instance_obj: require('../../assets/heap/objects/instance_obj.gif'),
    instance_obj_gc_root: require('../../assets/heap/objects/instance_obj_gc_root.gif'),
    class_package: require('../../assets/heap/objects/package.gif'),
    superclass: require('../../assets/heap/objects/superclass.gif'),
    class: require('../../assets/heap/objects/class.gif'),

    out: {
      class_obj: require('../../assets/heap/objects/out/class_obj.gif'),
      class: require('../../assets/heap/objects/out/class.gif'),
      class_mixed: require('../../assets/heap/objects/out/class_mixed.gif'),
      class_old: require('../../assets/heap/objects/out/class_out_old.gif'),
      class_obj_gc_root: require('../../assets/heap/objects/out/class_obj_gc_root.gif'),
      classloader_obj: require('../../assets/heap/objects/out/classloader_obj.gif'),
      classloader_obj_gc_root: require('../../assets/heap/objects/out/classloader_obj_gc_root.gif'),
      array_obj: require('../../assets/heap/objects/out/array_obj.gif'),
      array_obj_gc_root: require('../../assets/heap/objects/out/array_obj_gc_root.gif'),
      instance_obj: require('../../assets/heap/objects/out/instance_obj.gif'),
      instance_obj_gc_root: require('../../assets/heap/objects/out/instance_obj_gc_root.gif'),
    },

    in: {
      class_obj: require('../../assets/heap/objects/in/class_obj.gif'),
      class: require('../../assets/heap/objects/in/class.gif'),
      class_mixed: require('../../assets/heap/objects/in/class_mixed.gif'),
      class_old: require('../../assets/heap/objects/in/class_in_old.gif'),
      class_obj_gc_root: require('../../assets/heap/objects/in/class_obj_gc_root.gif'),
      classloader_obj: require('../../assets/heap/objects/in/classloader_obj.gif'),
      classloader_obj_gc_root: require('../../assets/heap/objects/in/classloader_obj_gc_root.gif'),
      array_obj: require('../../assets/heap/objects/in/array_obj.gif'),
      array_obj_gc_root: require('../../assets/heap/objects/in/array_obj_gc_root.gif'),
      instance_obj: require('../../assets/heap/objects/in/instance_obj.gif'),
      instance_obj_gc_root: require('../../assets/heap/objects/in/instance_obj_gc_root.gif'),
    },
  }
}

import {CLASS_TYPE, OBJECT_TYPE} from "./CommonType";

export function getIcon(isGCRoot, objType) {

  if (objType === OBJECT_TYPE.CLASS) {
    return isGCRoot ? ICONS.objects.class_obj_gc_root : ICONS.objects.class_obj;
  }

  if (objType === OBJECT_TYPE.CLASSLOADER) {
    return isGCRoot ? ICONS.objects.classloader_obj_gc_root : ICONS.objects.classloader_obj;
  }

  if (objType === OBJECT_TYPE.ARRAY) {
    return isGCRoot ? ICONS.objects.array_obj_gc_root : ICONS.objects.array_obj;
  }

  if (objType === OBJECT_TYPE.NORMAL) {
    return isGCRoot ? ICONS.objects.instance_obj_gc_root : ICONS.objects.instance_obj;
  }
}

export function getOutboundIcon(isGCRoot, objType) {

  if (objType === OBJECT_TYPE.CLASS) {
    return isGCRoot ? ICONS.objects.out.class_obj_gc_root : ICONS.objects.out.class_obj;
  }

  if (objType === OBJECT_TYPE.CLASSLOADER) {
    return isGCRoot ? ICONS.objects.out.classloader_obj_gc_root : ICONS.objects.out.classloader_obj;
  }

  if (objType === OBJECT_TYPE.ARRAY) {
    return isGCRoot ? ICONS.objects.out.array_obj_gc_root : ICONS.objects.out.array_obj;
  }

  if (objType === OBJECT_TYPE.NORMAL) {
    return isGCRoot ? ICONS.objects.out.instance_obj_gc_root : ICONS.objects.out.instance_obj;
  }
}

export function getInboundIcon(isGCRoot, objType) {

  if (objType === OBJECT_TYPE.CLASS) {
    return isGCRoot ? ICONS.objects.in.class_obj_gc_root : ICONS.objects.in.class_obj;
  }

  if (objType === OBJECT_TYPE.CLASSLOADER) {
    return isGCRoot ? ICONS.objects.in.classloader_obj_gc_root : ICONS.objects.in.classloader_obj;
  }

  if (objType === OBJECT_TYPE.ARRAY) {
    return isGCRoot ? ICONS.objects.in.array_obj_gc_root : ICONS.objects.in.array_obj;
  }

  if (objType === OBJECT_TYPE.NORMAL) {
    return isGCRoot ? ICONS.objects.in.instance_obj_gc_root : ICONS.objects.in.instance_obj;
  }
}

export function getClassRefInboundIcon(type) {
  if (type === CLASS_TYPE.NEW) {
    return ICONS.objects.in.class
  }

  if (type === CLASS_TYPE.MIXED) {
    return ICONS.objects.in.class_mixed

  }
  if (type === CLASS_TYPE.OLD_FAD) {
    return ICONS.objects.in.class_old
  }
}

export function getClassRefOutboundIcon(type) {
  if (type === CLASS_TYPE.NEW) {
    return ICONS.objects.out.class
  }

  if (type === CLASS_TYPE.MIXED) {
    return ICONS.objects.out.class_mixed

  }
  if (type === CLASS_TYPE.OLD_FAD) {
    return ICONS.objects.out.class_old
  }
}
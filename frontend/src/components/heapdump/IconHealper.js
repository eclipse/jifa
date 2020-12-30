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
    classloader: require('../../assets/heap/objects/out/classloader_obj.gif'),
    classloader_obj: require('../../assets/heap/objects/classloader_obj.gif'),
    classloader_obj_gc_root: require('../../assets/heap/objects/classloader_obj_gc_root.gif'),
    array_obj: require('../../assets/heap/objects/array_obj.gif'),
    array_obj_gc_root: require('../../assets/heap/objects/array_obj_gc_root.gif'),
    instance_obj: require('../../assets/heap/objects/instance_obj.gif'),
    instance_obj_gc_root: require('../../assets/heap/objects/instance_obj_gc_root.gif'),
    package: require('../../assets/heap/objects/package.gif'),
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

function attr_string_of(isGCRoot, type, isObj){
  let attr;
  switch (type) {
    case OBJECT_TYPE.CLASS:
      attr="class";break;
    case OBJECT_TYPE.CLASSLOADER:
      attr="classloader";break;
    case OBJECT_TYPE.ARRAY:
      attr="array";break;
    case OBJECT_TYPE.SUPERCLASS:
      attr="superclass";break;
    case OBJECT_TYPE.PACKAGE:
      attr="package";break;
    case OBJECT_TYPE.NORMAL:
    default:
      attr="instance";break;
  }
  if(isObj){
    attr+=(isGCRoot?"_obj_gc_root":"_obj");
  }
  return attr;
}
// ES6 master helps you!
export function getIcon(isGCRoot, type, isObj=true) {
  let attr = attr_string_of(isGCRoot, type, isObj)
  return ICONS.objects[attr];
}

export function getOutboundIcon(isGCRoot, objType, isObj=true) {
  let attr = attr_string_of(isGCRoot, objType, isObj)
  return ICONS.objects.out[attr];
}

export function getInboundIcon(isGCRoot, objType,  isObj=true) {
  let attr = attr_string_of(isGCRoot, objType, isObj)
  return ICONS.objects.in[attr];
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
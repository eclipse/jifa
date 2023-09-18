/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
import decorations_gc_root from '@/assets/heapdump/decorations/gc_root.gif';
import id from '@/assets/heapdump/id.gif';
import misc_sumIcon from '@/assets/heapdump/misc/sum.gif';
import misc_sumPlusIcon from '@/assets/heapdump/misc/sum_plus.gif';
import objects_array_obj from '@/assets/heapdump/objects/array_obj.gif';
import objects_array_obj_gc_root from '@/assets/heapdump/objects/array_obj_gc_root.gif';
import objects_class from '@/assets/heapdump/objects/class.gif';
import objects_class_obj from '@/assets/heapdump/objects/class_obj.gif';
import objects_class_obj_gc_root from '@/assets/heapdump/objects/class_obj_gc_root.gif';
import objects_classloader_obj from '@/assets/heapdump/objects/classloader_obj.gif';
import objects_classloader_obj_gc_root from '@/assets/heapdump/objects/classloader_obj_gc_root.gif';
import objects_in_array_obj from '@/assets/heapdump/objects/in/array_obj.gif';
import objects_in_array_obj_gc_root from '@/assets/heapdump/objects/in/array_obj_gc_root.gif';
import objects_in_class from '@/assets/heapdump/objects/in/class.gif';
import objects_in_class_old from '@/assets/heapdump/objects/in/class_in_old.gif';
import objects_in_class_mixed from '@/assets/heapdump/objects/in/class_mixed.gif';
import objects_in_class_obj from '@/assets/heapdump/objects/in/class_obj.gif';
import objects_in_class_obj_gc_root from '@/assets/heapdump/objects/in/class_obj_gc_root.gif';
import objects_in_classloader_obj from '@/assets/heapdump/objects/in/classloader_obj.gif';
import objects_in_classloader_obj_gc_root from '@/assets/heapdump/objects/in/classloader_obj_gc_root.gif';
import objects_in_instance_obj from '@/assets/heapdump/objects/in/instance_obj.gif';
import objects_in_instance_obj_gc_root from '@/assets/heapdump/objects/in/instance_obj_gc_root.gif';
import objects_instance_obj from '@/assets/heapdump/objects/instance_obj.gif';
import objects_instance_obj_gc_root from '@/assets/heapdump/objects/instance_obj_gc_root.gif';
import objects_out_array_obj from '@/assets/heapdump/objects/out/array_obj.gif';
import objects_out_array_obj_gc_root from '@/assets/heapdump/objects/out/array_obj_gc_root.gif';
import objects_out_class from '@/assets/heapdump/objects/out/class.gif';
import objects_out_class_mixed from '@/assets/heapdump/objects/out/class_mixed.gif';
import objects_out_class_obj from '@/assets/heapdump/objects/out/class_obj.gif';
import objects_out_class_obj_gc_root from '@/assets/heapdump/objects/out/class_obj_gc_root.gif';
import objects_out_class_old from '@/assets/heapdump/objects/out/class_out_old.gif';
import {
  default as objects_classloader,
  default as objects_out_classloader_obj
} from '@/assets/heapdump/objects/out/classloader_obj.gif';
import objects_out_classloader_obj_gc_root from '@/assets/heapdump/objects/out/classloader_obj_gc_root.gif';
import objects_out_instance_obj from '@/assets/heapdump/objects/out/instance_obj.gif';
import objects_out_instance_obj_gc_root from '@/assets/heapdump/objects/out/instance_obj_gc_root.gif';
import objects_package from '@/assets/heapdump/objects/package.gif';
import objects_superclass from '@/assets/heapdump/objects/superclass.gif';
import roots from '@/assets/heapdump/roots.gif';
import size from '@/assets/heapdump/size.gif';

export const ICONS = {
  id: id,
  size: size,
  roots: roots,
  misc: {
    sumIcon: misc_sumIcon,
    sumPlusIcon: misc_sumPlusIcon
  },
  decorations: {
    gc_root: decorations_gc_root
  },
  objects: {
    class_obj: objects_class_obj,
    class_obj_gc_root: objects_class_obj_gc_root,
    classloader: objects_classloader,
    classloader_obj: objects_classloader_obj,
    classloader_obj_gc_root: objects_classloader_obj_gc_root,
    array_obj: objects_array_obj,
    array_obj_gc_root: objects_array_obj_gc_root,
    instance_obj: objects_instance_obj,
    instance_obj_gc_root: objects_instance_obj_gc_root,
    package: objects_package,
    superclass: objects_superclass,
    class: objects_class,

    out: {
      class_obj: objects_out_class_obj,
      class: objects_out_class,
      class_mixed: objects_out_class_mixed,
      class_old: objects_out_class_old,
      class_obj_gc_root: objects_out_class_obj_gc_root,
      classloader_obj: objects_out_classloader_obj,
      classloader_obj_gc_root: objects_out_classloader_obj_gc_root,
      array_obj: objects_out_array_obj,
      array_obj_gc_root: objects_out_array_obj_gc_root,
      instance_obj: objects_out_instance_obj,
      instance_obj_gc_root: objects_out_instance_obj_gc_root
    },

    in: {
      class_obj: objects_in_class_obj,
      class: objects_in_class,
      class_mixed: objects_in_class_mixed,
      class_old: objects_in_class_old,
      class_obj_gc_root: objects_in_class_obj_gc_root,
      classloader_obj: objects_in_classloader_obj,
      classloader_obj_gc_root: objects_in_classloader_obj_gc_root,
      array_obj: objects_in_array_obj,
      array_obj_gc_root: objects_in_array_obj_gc_root,
      instance_obj: objects_in_instance_obj,
      instance_obj_gc_root: objects_in_instance_obj_gc_root
    }
  }
};

import { CLASS_TYPE, OBJECT_TYPE } from './type';

function attr_string_of(isGCRoot, type, isObj) {
  let attr;
  switch (type) {
    case OBJECT_TYPE.CLASS:
      attr = 'class';
      break;
    case OBJECT_TYPE.CLASSLOADER:
      attr = 'classloader';
      break;
    case OBJECT_TYPE.ARRAY:
      attr = 'array';
      break;
    case OBJECT_TYPE.SUPERCLASS:
      attr = 'superclass';
      break;
    case OBJECT_TYPE.PACKAGE:
      attr = 'package';
      break;
    case OBJECT_TYPE.NORMAL:
    default:
      attr = 'instance';
      break;
  }
  if (isObj) {
    attr += isGCRoot ? '_obj_gc_root' : '_obj';
  }
  return attr;
}

export function getIcon(isGCRoot, type, isObj = true) {
  let attr = attr_string_of(isGCRoot, type, isObj) as keyof typeof ICONS.objects;
  return ICONS.objects[attr];
}

export function getOutboundIcon(isGCRoot, objType, isObj = true) {
  let attr = attr_string_of(isGCRoot, objType, isObj);
  return ICONS.objects.out[attr];
}

export function getInboundIcon(isGCRoot, objType, isObj = true) {
  let attr = attr_string_of(isGCRoot, objType, isObj);
  return ICONS.objects.in[attr];
}

export function getClassRefInboundIcon(type) {
  if (type === CLASS_TYPE.NEW) {
    return ICONS.objects.in.class;
  }

  if (type === CLASS_TYPE.MIXED) {
    return ICONS.objects.in.class_mixed;
  }
  if (type === CLASS_TYPE.OLD_FAD) {
    return ICONS.objects.in.class_old;
  }
}

export function getClassRefOutboundIcon(type: number) {
  if (type === CLASS_TYPE.NEW) {
    return ICONS.objects.out.class;
  }

  if (type === CLASS_TYPE.MIXED) {
    return ICONS.objects.out.class_mixed;
  }

  if (type === CLASS_TYPE.OLD_FAD) {
    return ICONS.objects.out.class_old;
  }
}

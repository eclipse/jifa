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
export const PIE_COLORS = [
  [96, 127, 143],
  [98, 146, 147],
  [110, 138, 79],
  [140, 101, 87],
  [123, 96, 114],
  [101, 129, 120],
  [148, 132, 75],
  [150, 103, 110],

  [152, 173, 183],
  [154, 185, 185],
  [162, 180, 141],
  [181, 156, 147],
  [170, 152, 164],
  [156, 175, 168],
  [186, 176, 139],
  [188, 157, 162],

  [68, 105, 125],
  [21, 101, 112],
  [85, 118, 48],
  [119, 74, 57],
  [100, 68, 89],
  [73, 108, 96],
  [129, 110, 44],
  [132, 76, 84]
]

export const REMAINDER_COLOR= [220, 220, 220]

export function a2rgb(c) {
  let r = c[0]
  let g = c[1]
  let b = c[2]
  return '#' + r.toString(16) + g.toString(16) + b.toString(16)
}


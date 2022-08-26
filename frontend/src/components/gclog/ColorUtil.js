/********************************************************************************
 * Copyright (c) 2020,2022 Contributors to the Eclipse Foundation
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

export const colors = [
  "#ECA943",
  "#42436C",
  "#238E23",
  "#8F8FBD",
  "#70DB93",
  "#5C3317",
  "#9F5F9F",
  "#5F9F9F",
  "#FF7F00",
  "#9932CD",
  "#2F4F4F",
  "#7093DB",
  "#D19275",
  "#8E2323",
  "#CD7F32",
  "#DBDB70",
  "#527F76",
  "#93DB70",
  "#215E21",
  "#4E2F2F",
  "#9F9F5F",
  "#C0D9D9",
]

export function getIthColor(i) {
  return colors[i % colors.length]
}

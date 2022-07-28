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

const colors = [
  "#ECA943",
  "#42436C",
  "#238E23",
  "#8F8FBD",
  "#70DB93",
  "#5C3317",
  "#9F5F9F",
  "#B5A642",
  "#A67D3D",
  "#8C7853",
  "#A67D3D",
  "#5F9F9F",
  "#D98719",
  "#B87333",
  "#FF7F00",
  "#5C4033",
  "#2F4F2F",
  "#4A766E",
  "#4F4F2F",
  "#9932CD",
  "#871F78",
  "#6B238E",
  "#2F4F4F",
  "#97694F",
  "#7093DB",
  "#855E42",
  "#545454",
  "#856363",
  "#D19275",
  "#8E2323",
  "#CD7F32",
  "#DBDB70",
  "#C0C0C0",
  "#527F76",
  "#93DB70",
  "#215E21",
  "#4E2F2F",
  "#9F9F5F",
  "#C0D9D9",
  "#A8A8A8",
]

export function getIthColor(i) {
  return colors[i % colors.length]
}

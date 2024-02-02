/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
export const toReadableValue = (unit: string, value: number) => {
  if (unit === 'ns') {
    let result = '';

    // to ms
    value = Math.round(value / 1000000);
    const ms = value % 1000;
    if (ms > 0) {
      result = ms + 'ms'
    }

    // to second
    value = Math.floor(value / 1000);
    const s = value % 60;
    if (s > 0) {
      if (result.length > 0) {
        result = s + 's ' + result;
      } else {
        result = s + 's';
      }
    }

    // to minute
    value = Math.floor(value / 60);
    const m = value;
    if (m > 0) {
      if (result.length > 0) {
        result = m.toLocaleString() + 'm ' + result;
      } else {
        result = m.toLocaleString() + 'm';
      }
    }

    if (result.length === 0) {
      result = '0ms';
    }

    return result;
  } else if (unit === 'byte') {
    let result = '';

    // to Kilobytes
    value = Math.round(value / 1024);
    const kb = value % 1024;
    if (kb > 0) {
      result = kb + 'KB';
    }

    // to Megabytes
    value = Math.floor(value / 1024);
    const mb = value % 1024;
    if (mb > 0) {
      if (result.length > 0) {
        result = mb + 'MB ' + result;
      } else {
        result = mb + 'MB';
      }
    }

    // to Gigabyte
    value = Math.floor(value / 1024);
    const gb = value % 1024;
    if (gb > 0) {
      if (result.length > 0) {
        result = gb + 'GB ' + result;
      } else {
        result = gb + 'GB';
      }
    }

    // to Terabyte
    value = Math.floor(value / 1024);
    const tb = value;
    if (tb > 0) {
      if (result.length > 0) {
        result = tb + 'TB ' + result;
      } else {
        result = tb + 'TB';
      }
    }

    if (result.length === 0) {
      result = '0B';
    }

    return result;
  } else {
    return value.toLocaleString();
  }
}

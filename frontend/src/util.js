/********************************************************************************
 * Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation
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
function nullOrUndefined(v) {
  return v === null || v === undefined;
}

export function toReadableSizeWithUnit(bytes) {
  if (nullOrUndefined(bytes)) return ''
  if (bytes <= 1024) return bytes.toLocaleString() + "B"
  let k = 1024,
      suffix = ['B', 'K', 'M', 'G'],
      i = Math.floor(Math.log(bytes) / Math.log(k))

  return (bytes / Math.pow(k, i)).toPrecision(3).toLocaleString() + suffix[i]
}

export function toReadableSizeWithUnitFormatter(r, c, cellValue) {
  return toReadableSizeWithUnit(cellValue)
}

export function toReadableCountWithUnit(counts) {
  if (nullOrUndefined(counts)) return ''
  if (counts <= 1000) return counts.toLocaleString()
  let k = 1000,
      suffix = ['', 'k', 'm', 'g', 't', 'p', 'e', 'z', 'y'],
      i = Math.floor(Math.log(counts) / Math.log(k))

  return (counts / Math.pow(k, i)).toPrecision(4).toLocaleString() + suffix[i]
}

export function toReadableCount(counts) {
  if (nullOrUndefined(counts)) return ''
  return counts.toLocaleString();
}

export function toReadableCountFormatter(r, c, cellValue) {
  return toReadableCount(cellValue)
}

const SERVICE_PREFIX = '/jifa-api'

export function service(suffix) {
  return SERVICE_PREFIX + suffix
}

export function heapDumpService(file, api) {
  return SERVICE_PREFIX + '/heap-dump/' + file + '/' + api;
}

export function gclogService(file, api) {
  return SERVICE_PREFIX + '/gc-log/' + file + '/' + api;
}

export function threadDumpService(file, api) {
  return SERVICE_PREFIX + '/thread-dump/' + file + '/' + api;
}

export function matchSearch(data,val){
  let temp = [];
  for(let index in data){
    for(let prop in data[index]){
      // Do not use strict equal operator
      if(data[index][prop]==val){
        temp.push(data[index]);
      }
    }
  }
  return temp;
}

// e.g. formatTime(1545903266795, 'Y-M-D h:m:s')
export function formatTime (number, format) {
  let time = new Date(number)
  let newArr = []
  let formatArr = ['Y', 'M', 'D', 'h', 'm', 's']
  newArr.push(time.getFullYear())
  newArr.push(formatNumber(time.getMonth() + 1))
  newArr.push(formatNumber(time.getDate()))

  newArr.push(formatNumber(time.getHours()))
  newArr.push(formatNumber(time.getMinutes()))
  newArr.push(formatNumber(time.getSeconds()))

  for (let i in newArr) {
    format = format.replace(formatArr[i], newArr[i])
  }
  return format;
}

function formatNumber (n) {
  n = n.toString()
  return n[1] ? n : '0' + n;
}

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
export function toSizeString(bytes) {
  if (bytes < 0) {
    return "N/A"
  }
  if (bytes <= 1024) return bytes + " B"
  let k = 1024,
      suffix = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'],
      i = Math.floor(Math.log(bytes) / Math.log(k))

  const num = bytes / Math.pow(k, i)
  return (num >= 1000 ? num.toFixed(0) : num.toPrecision(3)) + ' ' + suffix[i]
}

export function toCountString(counts) {
  if (counts <= 1000) return counts + " "
  let k = 1000,
      suffix = [' ', 'k', 'm', 'g', 't', 'p', 'e', 'z', 'y'],
      i = Math.floor(Math.log(counts) / Math.log(k))

  return (counts / Math.pow(k, i)).toPrecision(4) + ' ' + suffix[i]
}

export function fromSizeString(str) {
  let k = 1024, suffix = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
  let [a, b] = str.split(' ')
  if (suffix.indexOf(b) === -1) {
    throw "should not reach here";
  }
  return parseFloat(a) * Math.pow(k, suffix.indexOf(b))
}

export function fromCountString(str) {
  let k = 1000, suffix = [' ', 'k', 'm', 'g', 't', 'p', 'e', 'z', 'y']
  let [a, b] = str.split(' ')
  if (b === '') {
    return parseFloat(a);
  }
  if (suffix.indexOf(b) === -1) {
    throw "should not reach here";
  }
  return parseFloat(a) * Math.pow(k, suffix.indexOf(b))
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

// e.g 100,000 (ms) -> '1.67min'
export function formatTimePeriod(time) {
  // negative means not available
  if (time < 0 || typeof time === "undefined") {
    return "N/A"
  }
  if (time < 0.001) {
    return "0"
  }
  if (time < 1) {
    return `${time.toFixed(3)}ms`
  }
  const units = ["ms", "s", "min", "h"]
  const gap = [1000, 60, 60]
  let i;
  for (i = 0; i < 3; i++) {
    if (time > gap[i]) {
      time /= gap[i]
    } else {
      break
    }
  }
  return (time >= 1000 ? Math.round(time) : time.toPrecision(3)) + " " + units[i];
}

export function toSizeSpeedString(bytesPerMs) {
  if (bytesPerMs < 0) {
    return "N/A"
  }
  return toSizeString(bytesPerMs * 1000) + "/s"
}

function formatNumber (n) {
  n = n.toString()
  return n[1] ? n : '0' + n;
}

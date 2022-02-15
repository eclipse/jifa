/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

export function frameToString(frame) {
  let r = frame.class + "." + frame.method
  if (frame.line > 0) {
    r += " : " + frame.line
  } else if (frame.sourceType === "NATIVE_METHOD") {
    r += " (native)"
  }
  return r
}

function addressToString(address) {
  if (address === -1) {
    return "<Unknown Address>"
  }

  return "<0x" + address.toString(16) + ">"
}

export function rawMonitorToString(monitor) {
  let r = addressToString(monitor.address)
  if (monitor.class) {
    if (monitor.classInstance) {
      r += " (a java.lang.Class for " + monitor.class + ")"
    } else {
      r += " (a " + monitor.class + ")"
    }
  }
  return r
}

export function monitorsToStrings(monitors) {
  let strings = []
  monitors.forEach(monitor => {
    strings.push(monitorToString(monitor))
  })
  return strings
}

export function monitorToString(monitor) {
  let state = monitor.state
  if (state === "WAITING_ON") {
    let s = "waiting on " + addressToString(monitor.address) + " "
    if (monitor.classInstance) {
      s += "(a java.lang.Class for " + monitor.class + ")"
    } else {
      s += "(a " + monitor.class + ")"
    }
    return s
  } else if (state === "WAITING_TO_RE_LOCK") {
    let s = "waiting to re-lock in wait() " + addressToString(monitor.address) + " "
    if (monitor.classInstance) {
      s += "(a java.lang.Class for " + monitor.class + ")"
    } else {
      s += "(a " + monitor.class + ")"
    }
    return s
  } else if (state === "WAITING_ON_NO_OBJECT_REFERENCE_AVAILABLE") {
    return "waiting on <no object reference available>"
  } else if (state === "PARKING") {
    return "parking to wait for " + addressToString(monitor.address) + " (a " + monitor.class + ")"
  } else if (state === "WAITING_ON_CLASS_INITIALIZATION") {
    return "waiting on the Class initialization monitor for " + monitor.class
  } else if (state === "LOCKED") {
    let s = "locked " + addressToString(monitor.address) + " "
    if (monitor.classInstance) {
      s += "(a java.lang.Class for " + monitor.class + ")"
    } else {
      s += "(a " + monitor.class + ")"
    }
    return s
  } else if (state === "WAITING_TO_LOCK") {
    let s = "waiting to lock " + monitorToString(monitor.address) + " "
    if (monitor.classInstance) {
      s += "(a java.lang.Class for " + monitor.class + ")"
    } else {
      s += "(a " + monitor.class + ")"
    }
    return s
  } else if (state === "ELIMINATED_SCALAR_REPLACED") {
    return "eliminated <owner is scalar replaced> (a " + monitor.class + ")"
  } else if (state === "ELIMINATED") {
    let s = "eliminated " + monitorToString(monitor.address) + " "
    if (monitor.classInstance) {
      s += "(a java.lang.Class for " + monitor.class + ")"
    } else {
      s += "(a " + monitor.class + ")"
    }
    return s
  }
}
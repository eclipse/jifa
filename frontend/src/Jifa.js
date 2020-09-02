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
export default class JifaGlobal {
  static prod() {
    return process.env.NODE_ENV === 'production'
  }

  static dev() {
    return process.env.NODE_ENV === 'development'
  }

  static fileManagement() {
    // default enabled, but can be disabled by setting 'false'
    return process.env.VUE_APP_JIFA_FILE_MGMT !== 'false'
  }
}

/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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
module.exports = {
  devServer: {
    historyApiFallback: {
      disableDotRule: true
    },
    noInfo: true,
    port: 8089,
    proxy: {
      '/jifa-api': {
        target: 'http://127.0.0.1:' + (process.env.JIFA_API_PORT || 8102)
      },
    }
  },
  publicPath: './'
}

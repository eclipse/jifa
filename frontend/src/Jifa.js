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
import axios from "axios";

export default class JifaGlobal {
  static prod() {
    return process.env.NODE_ENV === 'production'
  }

  static dev() {
    return process.env.NODE_ENV === 'development'
  }

  static save_back_url(url) {
    window.localStorage.setItem("jifa_back_url", url)
  }

  static back_url() {
    return window.localStorage.getItem("jifa_back_url")
  }

  static clean_back_url() {
    window.localStorage.removeItem("jifa_back_url")
  }

  static save_jifa_token(token) {
    window.localStorage.setItem("jifa_token", token)
  }

  static jifa_token() {
    return window.localStorage.getItem("jifa_token")
  }

  static init_authorization_header() {
    let jifa_token = this.jifa_token()
    if (jifa_token) {
      axios.defaults.headers.common['Authorization'] = 'Bearer ' + jifa_token;
    }
  }

  static reset_authorization_header(new_token) {
    if (new_token){
      this.save_jifa_token(new_token)
      axios.defaults.headers.common['Authorization'] = 'Bearer ' + new_token;
    }
  }

  static get_authorization_header() {
    return axios.defaults.headers.common['Authorization']
  }

  static init_locale(i18n, def) {
    let locale = window.localStorage.getItem("jifa_i18n")
    i18n.locale = locale ? locale : def;
  }

  static set_locale(i18n, v) {
    window.localStorage.setItem("jifa_i18n", v)
    i18n.locale = v
  }
}


export var USER = {
  id: 1,
  nickname: 'Jifa',
  avatar: require('./assets/avatar.png'),
  admin: false,
  loaded: false
}

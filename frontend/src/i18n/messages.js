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
import element_ui_enLocale from 'element-ui/lib/locale/lang/en'
import element_ui_zhLocale from 'element-ui/lib/locale/lang/zh-CN'

import en from './en'
import cn from './cn'

export default {
  en: {
    ...en,
    ...element_ui_enLocale,
  },

  cn: {
    ...cn,
    ...element_ui_zhLocale
  }
}
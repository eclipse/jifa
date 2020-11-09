<!--
    Copyright (c) 2020 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template>
  <div>
  </div>
</template>

<script>
  import {Loading} from "element-ui";
  import {service} from "../../util"
  import axios from "axios";

  export default {
    methods: {
      auth() {
        let loadingInstance = Loading.service({fullscreen: true})

        let params = new FormData()
        params.append("username", "admin")
        params.append("password", "admin")
        axios.post(service("/auth"), new URLSearchParams(params)).then(resp => {
          this.$jifa.reset_authorization_header(resp.data.token)
          loadingInstance.close()

          let back_url = this.$jifa.back_url()
          if (back_url) {
            this.$jifa.clean_back_url()
            window.location.href = back_url
          } else {
            this.$router.push({name: "finder"})
          }
        })
      }
    },
    mounted() {
      this.auth()
    }
  }
</script>
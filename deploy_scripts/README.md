<!--
    Copyright (c) 2021 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->

## Default Pattern
### 1. Master+Worker
If you want to deploy classic `one master`+`several workers` application, you can use
```bash
$ ./build_jifa.sh
$ ./deploy_jifa.sh
```

### 2. Worker Only
If you want to deploy worker only, run script directly
```bash
$ ./build_worker.sh
$ ./deploy_worker.sh
```
Then visit http://127.0.0.1:8102 in your browser. This is the simplest way to get started with Jifa.

## K8S Pattern
For K8S pattern, you can launch a single node cluster using `minikube start --driver=hyperkit`, then deploy Jifa by the following commands:
```bash
$ ./deploy.sh
```

Don't know which pattern is desired for you? Please free feel to file an `Issue` to ask for help, or try the simplest worker only mode.

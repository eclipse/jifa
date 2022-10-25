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
Before deployment, please make sure you have build Jifa.

### 1. Master+Worker
If you want to deploy classic `one master`+`several workers` application, you can use
```bash
$ cd deploy/default_pattern
$ ./deploy_jifa.sh
```
Then visit http://127.0.0.1:80 in your browser. This contains full features of Jifa, we recommend using this for enterprise use.

### 2. Worker Only
If you want to deploy worker only, run script directly
```bash
$ cd deploy
$ ./deploy_worker.sh
```
Then visit http://127.0.0.1:8102 in your browser. This is the simplest way to get started with Jifa, we recommend using this for personal use.

## K8S Pattern
For K8S pattern, you can launch a single node cluster using `minikube start --driver=hyperkit`, then deploy Jifa by the following commands:
```bash
$ cd scripts/docker_images
$ ./build_image.sh
$ cd ../..
$ cd deploy/k8s_pattern
$ ./deploy.sh
```
We recommend using this for enterprise use, especially for people who want to save machine resources, and you already have a k8s cluster.

Don't know which pattern is desired for you? Please free feel to file an `Issue` to ask for help, or try the simplest *Worker only* mode.

<!--
    Copyright (c) 2020 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->

# Eclipse Jifa
[![License](https://img.shields.io/badge/License-EPL%202.0-green.svg)](https://opensource.org/licenses/EPL-2.0)

[Eclipse Jifa](https://eclipse.org/jifa) is a web application based on the Eclipse Memory Analyser Tooling (MAT)
that provides HTTP services so that users can view the heap dump files analysis through a browser.
Users can deploy Jifa to their production environments, and share the same analysis result to different users
via their browsers, we believe it's a more convenient way to troubleshoot Java heap issues.

## Introduction
Eclipse Jifa uses Vert.x as the main backend framework, and uses Vue 2.0 as the frontend framework.

Currently, supported features:

Heap dump Analysis:
- Overview
- Leak Suspects
- GC Roots
- Dominator Tree
- Thread Overview
- OQL
- Other features

![Jifa Sample](https://raw.githubusercontent.com/wiki/eclipse/jifa/resources/jifa-sample.jpg)


## What's the goal?
We believe that many companies have encountered problems when troubleshooting Java problems
in their production environments, and we hope that Jifa will grow into a popular product to
help developers quickly resolve production problems.

Looking forward to more users and contributors :-)

## How do I interact with the community?
- Join the Eclipse Jifa developer community [mailing list](https://accounts.eclipse.org/mailing-list/jifa-dev).
  The community primarily uses this list for project announcements and administrative discussions amongst committers.
  Questions are welcome here as well.
- Ask a question or start a discussion via the [GitHub issue](https://github.com/eclipse/jifa/issues).
- Slack channel: [Eclipse Jifa](https://eclipsejifa.slack.com)

## Quick start
Jifa provides two modes of running: worker-only mode and full cluster mode. The following shows how to use the these two mode, respectively.

## 1. Worker-only mode
Only using worker as a standalone application is a simple and lightweight mode. 
In this mode, we only need to deploy the front end and worker side without any database configuration. 
To use this mode, we need to forward the http requests to the workers:
```bash
$ ./gradlew clean buildWorker
$ cd demo
$ ./run_worker.sh
```

## 2. Full cluster mode
The other mode is to start the entire Jifa, which includes worker and master. 
This mode needs to set up the database in advance. 

Here we have prepared an example to demonstrate how to get started. First, configure the database:
```bash
$ cd demo
$ docker-compose build
$ docker-compose up # start mysql server
```
Then build the Jifa:
``` bash
$ ./gradlew clean buildJifa
```
Artifacts can be found in the `./deploy` directory. 
In production mode, we could use `nginx` as a static front-end resource server, 
and then start multiple workers and at least one master.

For the sake of simplicity, we demonstrate how to start them in development mode:

+ `Frontend`: `cd frontend && npm run serve`
+ `Master node` : `./gradlew :backend:master:run`
+ `Worker node` : `./gradlew :backend:worker:run` 

This would work for further developing and testing. 

# Documents
<details>
<summary>1. Jifa Customization</summary>

[1. Jifa Customization](./docs/customization.md)
</details>

<details>
<summary>2. Contribution</summary>

If you would like to contribute to Jifa, please check out the [contribution guide](./docs/contribution.md) for more information.
</details>
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

## Goal
We believe that many companies have encountered problems when troubleshooting Java problems
in their production environments, and we hope that Jifa will grow into a popular product to
help developers quickly address production problems.

Looking forward to more users and contributors :-)

## Links
- Join the Eclipse Jifa developer community [mailing list](https://accounts.eclipse.org/mailing-list/jifa-dev).
  The community primarily uses this list for project announcements and administrative discussions amongst committers.
  Questions are welcome here as well.
- **Ask a question or start a discussion via the [GitHub issue](https://github.com/eclipse/jifa/issues).(Recommend)**
- Slack channel: [Eclipse Jifa](https://eclipsejifa.slack.com)

## Quick start
Jifa provides two modes of running: worker-only mode and full cluster mode. The following shows how to use the these two mode, respectively.

## 1. Worker-only mode(Recommend)
Only using worker as a standalone application is a simple and lightweight mode. 
In this mode, we only need to deploy the front end and worker side without any database configuration. 
To use this mode, we need to forward the http requests to the workers:
```bash
$ ./gradlew clean
$ ./gradlew buildWorker
$ cd demo
$ ./run_worker.sh
```

## 2. Cloud mode(Experimental Feature)
Running the following command line to start k8s based cloud master:
```bash
$ cd cloud
$ ./run.sh
```
Master will create worker only when necessary.

# Documents
+ 1. [Jifa Customization](CUSTOMIZATION.md)
+ 2. [Contribution Guide](CONTRIBUTING.md)

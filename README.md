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

[Eclipse Jifa](https://eclipse.org/jifa) is open-source software for better troubleshooting common problems that occurred in Java applications.

Many of the useful tools are client-based. When faced with problems in the production environment or the cloud environment, such tools cannot be used directly due to network or resource problems. Jifa provides a web solution, allowing developers to use the browser to troubleshoot.

The following features are supported:

- [Heap Dump Analysis](backend/heap-dump-analyzer/README.md)

- [GC Log Analysis](backend/gc-log-analyzer/README.md)

- [Thread Dump Analysis](backend/thread-dump-analyzer/README.md)

The backend of Jifa uses Vert.x as the main framework and consists of two modules:

- Master
    - manage workers and route the requests from browser to the workers
- Worker
    - do the real analysis work
  
The frontend of Jifa uses Vue as the main framework.

## Getting Started

### Build

- Prerequisites
  - JDK 8, and make sure $JAVA_HOME is set properly

    ```
    Jifa uses the plugin 'com.diffplug.p2.asmaven' to get MAT's dependencies.
    This plugin can only run on JRE 8 now, so we need to set $JAVA_HOME to JDK8.
    While other modules depend on JDK11+, Gradle handles this for us correctly.
    ```
  - npm

- Build All
  
  ```bash
  $ ./gradlew buildJifa
  ```

- Build Worker Only

  ```bash
  $ ./gradlew buildWorker
  ```

### Run & Deploy

- Master & Worker

  - Default pattern
    ```bash
    $ cd deploy/default_pattern
    $ ./deploy_jifa.sh
    ```

  - K8S pattern, workers are scheduled by K8S
    ```bash
    $ cd deploy/k8s_pattern
    $ ./deploy.sh
    ```
    
- Worker Only
  ```bash
  $ cd deploy
  $ ./depoy_worker.sh
  ```

See [deployment document](deploy/README.md) for more details.

## Quick Demo

```bash
$ docker pull jifadocker/jifa-worker:demo
$ docker run -p 8102:8102 jifadocker/jifa-worker:demo
```

**Note:**  if running Apple's M1 Max chip, include the `--platform linux/amd64` switch after the `run` command.

Then, you can visit Jifa at `http://localhost:8102`

## Documents

- [Jifa Customization](CUSTOMIZATION.md)
- [Contribution Guide](CONTRIBUTING.md)
    
## Links

- Join the Eclipse Jifa developer community [mailing list](https://accounts.eclipse.org/mailing-list/jifa-dev).
  The community primarily uses this list for project announcements and administrative discussions amongst committers.
  Questions are welcome here as well.
- **Ask a question or start a discussion via the [GitHub issue](https://github.com/eclipse/jifa/issues).(Recommend)**
- Slack channel: [Eclipse Jifa](https://eclipsejifa.slack.com)
- 钉钉中文交流群

  <div>
    <img src=https://user-images.githubusercontent.com/33491035/226314386-e1cf71d4-8429-4e4c-bdc0-c511a9009ee1.JPG width=25%/>
  </div>

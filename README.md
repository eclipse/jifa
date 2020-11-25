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
- Ask a question or start a discussion via a [GitHub issue](https://github.com/eclipse/jifa/issues).
- Slack channel: [Eclipse Jifa](https://eclipsejifa.slack.com)

## Quick start
```
./gradlew build

cd build/distributions && unzip jifa-0.1.zip && cd jifa-0.1

./bin/worker

Jifa will now be reachable at http://localhost:8102.
```

## Customizing JIFA

Some options are provided to configure JIFA without modification to the source code.

### Frontend configuration

Frontend can be configured by modifying the `config.js` file provided in the application webroot, in the same
location as the `index.html` file.

A [sample config.js file](frontend/public/config.js) is provided with JIFA which you can edit.

### Backend customization

#### Configuration options

A configuration file can be specified as an environment variable.

```
export WORKER_OPTS=-Djifa.worker.config=/path/to/worker-config.json
./bin/worker
...
```

A sample configuration file is here:
```
{
  "server.host": "0.0.0.0",
  "server.port": 7101,
  "server.uploadDir": "/mnt/data/uploads",
  "api.prefix": "/jifa-api",
  "hooks.className": "com.yourco.JifaHooksImplementation",
  "enableAutoCleanExpiredCache": true,
  "expireMinutesAfterAccess": 30,
  "cleanupIntervalInMinutes": 10
}
```

If you choose to provide a configuration file, the `api.prefix` is the only required value. Otherwise,
defaults will apply. If you do not provide a configuration, defaults will apply. For more specific
customization, see next section.

#### Overriding HTTP server, route, and file mapping

The backend can be configured; JIFA has a number of hook points where it will call some code that you provide.

With hooks you can:
- Customize the HTTP server options
- Configure HTTP server routes to add authentication, error handling, health check URLs, etc.
- Customize the layout of heap files on the local file system.

To do so, you need to set configuration to refer to a new class which provides your custom implementations.
You can [provide the implementations by implementing this class](backend/common/src/main/java/org/eclipse/jifa/common/JifaHooks.java)
and then updating configuration file.

In the configuration file, provide a hooks class name to use it and it will be loaded at service startup. See
the `hooks.className` key. The JAR containing your class needs to be present on the classpath. You can use
`export WORKER_OPTS="-Djifa.worker.config=/path/to/config.json -cp /path/to/hook.jar"`.

You will need to extract the `common.jar` from the build process to get access to the JifaHooks interface.

## Contributing
If you would like to contribute to Jifa, please check out the [contributing guide][contrib] for more information.

[contrib]: CONTRIBUTING.md

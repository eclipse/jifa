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

## What's the goal
We believe that many companies have encountered problems when troubleshooting Java problems
in their production environments, and we hope that Jifa will grow into a popular product to
help developers quickly resolve production problems.

Looking forward to more users and contributors : )

## How do I interact with the community?
- Join the Eclipse Jifa developer community [mailing list](https://accounts.eclipse.org/mailing-list/jifa-dev).
  The community primarily uses this list for project announcements and administrative discussions amongst committers.
  Questions are welcome here as well.
- Ask a question or start a discussion via a [GitHub issue](https://github.com/eclipse/jifa/issues).
- Slack channel: TBD


## Quick start
```
./gradlew build

cd build/distributions && unzip jifa-0.1.zip && cd jifa-0.1

./bin/worker

Jifa will now be reachable at http://localhost:8102.
```

## Contributing
If you would like to contribute to Jifa, please check out the [contributing guide][contrib] for more information.

[contrib]: CONTRIBUTING.md

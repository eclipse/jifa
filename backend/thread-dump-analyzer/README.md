<!--
    Copyright (c) 2022 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->

# Thread Dump Analysis

A thread dump is a snapshot of all the threads of the Java application at a certain moment.

Analyzing thread dump can help troubleshoot problems like thread leaks, deadlocks.

### Supported Format

- Output of `jstack` command
  - the format with `-m` option is not supported now

### Feature List

- Basic Information
- Thread Overview
- Thread Group Overview
- Java Monitor
- Stack Trace
- Raw content
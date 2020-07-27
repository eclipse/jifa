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
- Some other features  

## Quick start
```
./gradlew build

cd build/distributions && unzip jifa-0.1.zip && cd jifa-0.1

./bin/worker

Jifa will now be reachable at http://localhost:8102.
```

## Licenses
[Eclipse Public License 2.0](https://projects.eclipse.org/license/epl-2.0)

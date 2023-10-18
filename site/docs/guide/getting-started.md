# Getting Started

## Docker

```shell
$ docker run -p 8102:8102 eclipsejifa/jifa
```

## jifa.sh

```shell
$ curl -fsSL https://raw.githubusercontent.com/eclipse/jifa/main/jifa.sh | bash

# Change the server port
$ curl -fsSL https://raw.githubusercontent.com/eclipse/jifa/main/jifa.sh | bash -s -- -p <port>

# Analyze local files
$ curl -fsSL https://raw.githubusercontent.com/eclipse/jifa/main/jifa.sh | bash -s -- <file1 path> <file2 path> ...

# Set JVM Options
curl -fsSL https://raw.githubusercontent.com/eclipse/jifa/main/jifa.sh | bash -s -- --jvm-options "<JVM options>"
```

::: tip Tip
[jifa.sh](https://github.com/eclipse/jifa/blob/main/jifa.sh) encapsulates `docker` command.
If you prefer to use the `docker` directly and also want to modify the server port or analyze local files,
please refer to it.
:::

## From Source

### Prerequisites

- JDK 17+
- Node.js 18+

### Run

```shell
# run as standalone worker 
$ ./gradlew runStandaloneWorker

# run as master
$ ./gradlew runMaster

# run as static worker
$ ./gradlew runStaticWorker
```

You can also run these tasks in IDE.

For the introduction regarding the roles, please refer to [deployment](./deployment).

### Build

```shell
$ ./gradlew build

# skip test
$ ./gradlew build -x test
```

The output can be found in `./server/build/distributions`.

### Build Docker Image

Use [Dockerfile](https://github.com/eclipse/jifa/blob/main/Dockerfile) in the project root directory.
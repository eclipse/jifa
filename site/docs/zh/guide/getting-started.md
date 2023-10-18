# 快速上手

## Docker

```shell
$ docker run -p 8102:8102 eclipsejifa/jifa
```

## jifa.sh

```shell
$ curl -fsSL https://raw.githubusercontent.com/eclipse/jifa/main/jifa.sh | bash

# 修改服务端口
$ curl -fsSL https://raw.githubusercontent.com/eclipse/jifa/main/jifa.sh | bash -s -- -p <port>

# 分析本地文件
$ curl -fsSL https://raw.githubusercontent.com/eclipse/jifa/main/jifa.sh | bash -s -- <file1 path> <file2 path> ...

# 设置 JVM 参数
curl -fsSL https://raw.githubusercontent.com/eclipse/jifa/main/jifa.sh | bash -s -- --jvm-options "<JVM options>"
```

::: tip 贴士
[jifa.sh](https://github.com/eclipse/jifa/blob/main/jifa.sh) 脚本内封装了 docker 启动命令，如果你想直接使用 docker
命令并修改服务端口或分析本地文件，可参考此脚本。
:::

## 从源码开始

### 环境要求

- JDK 17+
- Node.js 18+

### 运行

```shell
# 以 standalone worker 角色运行 
$ ./gradlew runStandaloneWorker

# 以 master 角色运行
$ ./gradlew runMaster

# 以 static worker 角色运行
$ ./gradlew runStaticWorker
```

你也可以在 IDE 中运行这些任务。

关于运行角色的介绍，请参考[部署](./deployment.md)。

### 构建

```shell
$ ./gradlew build

# 跳过测试
$ ./gradlew build -x test
```

构建结果位于 `./server/build/distributions` 目录。

### 构建 Docker 镜像

使用项目根目录中的 [Dockerfile](https://github.com/eclipse/jifa/blob/main/Dockerfile)。
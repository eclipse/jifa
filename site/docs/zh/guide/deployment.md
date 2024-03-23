# 部署

## 单机模式

将 [role](./configuration#role) 设置为 `STANDALONE_WORKER` 时，Jifa 将以单机模式运行。

在此模式下，默认会使用 H2 作为数据库。

适用场景：

- 分析本地文件。
- 部署在一台高配置的服务器中。

### 部署步骤

1. [构建](./getting-started#构建)完成后，将产物 `jifa.zip`（或 `jifa.tar`）上传到目标环境。
2. 解压：`unzip jifa.zip` 或 `tar -xf jifa.tar`
3. 运行：`./jifa/bin/jifa`

## 集群模式

集群模式需要准备一个 MySQL 数据库。

集群中有三类运行角色：

- `MASTER`：主节点，负责接收分析请求，并将请求转发到相应的工作节点。
- `STATIC_WORKER`：静态工作节点，负责处理分析请求。
- `ELASTIC_WORKER`：弹性工作节点，负责处理分析请求。

适用场景：

- 搭建平台供多个团队使用，且有较多的机器资源。
- 与其他系统进行集成。

### 部署集群

可参考 [cluster.yml](https://github.com/eclipse/jifa/blob/main/cluster.yml)。

命令：`kubectl apply -f cluster.yml`。

如果集群没有默认的 `Storage Class`，需要修改 `cluster.yml` 进行设置。

### 部署静态集群（仅包含主节点和静态工作节点）

构建和解压步骤和单机模式一样。

- 启动 `MASTER` 节点

  ```shell
  # 设置数据库环境变量
  $ export MYSQL_HOST=<host>
  $ export MYSQL_DATABASE=<name>
  $ export MYSQL_USER=<username>
  $ export MYSQL_PASSWORD=<password>

  $ ./jifa/bin/jifa --jifa.role=master
  ```

- 启动一个或多个 `STATIC_WORKER` 节点

  ```shell
  # 设置数据库环境变量
  $ export MYSQL_HOST=<host>
  $ export MYSQL_DATABASE=<name>
  $ export MYSQL_USER=<username>
  $ export MYSQL_PASSWORD=<password>

  $ ./jifa/bin/jifa --jifa.role=static_worker
  ```

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

集群中有两类运行角色：

- `MASTER`：主节点，负责接收分析请求，并将请求转发到相应的 `WORKER`。
- `WORKER`：工作节点，负责处理分析请求。

根据调度策略的不同，集群分为：

- 弹性集群：`WORKER` 节点按需调度。用户需要准备一个 K8S 集群。`WORKER` 节点的 `role` 为 `ELASTIC_WORKER`。
- 静态集群：需要事先启动 `WORKER` 节点。`WORKER` 节点的 `role` 为 `STATIC_WORKER`。

适用场景：

- 搭建平台供多个团队使用，且有较多的机器资源。
- 与其他系统进行集成。

### 弹性集群的部署步骤

可参考 [cluster.yml](https://github.com/eclipse/jifa/blob/main/cluster.yml)。

命令：`kubectl apply -f cluster.yml`。

### 静态集群的部署步骤

构建和解压步骤和单机模式一样。

- 启动 `MASTER` 节点

  ```shell
  # 设置数据库环境变量
  $ export MYSQL_HOST=<host>
  $ export MYSQL_DATABASE=<name>
  $ export MYSQL_USER=<username>
  $ export MYSQL_PASSWORD=<password>

  # 启动 MASTER，将调度策略设置为 static
  $ ./jifa/bin/jifa --jifa.role=master --jifa.scheduling-strategy=static
  ```

- 启动 `STATIC_WORKER` 节点

  ```shell
  # 设置数据库环境变量
  $ export MYSQL_HOST=<host>
  $ export MYSQL_DATABASE=<name>
  $ export MYSQL_USER=<username>
  $ export MYSQL_PASSWORD=<password>

  # 启动 STATIC_WORKER
  $ ./jifa/bin/jifa --jifa.role=static_worker
  ```

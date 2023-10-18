# 配置

本页梳理了服务端支持的配置。

各配置项的默认值由 [Configuration.java](https://github.com/eclipse/jifa/blob/main/server/src/main/java/org/eclipse/jifa/server/Configuration.java) 或默认配置文件 [application.yml](https://github.com/eclipse/jifa/blob/main/server/src/main/resources/application.yml) 提供。

通过启动参数 `--jifa.<key>=<value>` 或指定配置文件进行设置。

## role

运行角色，参考[部署](./deployment.md)。

类型：Enum

- `STANDALONE_WORKER`
- `MASTER`
- `ELASTIC_WORKER`
- `STATIC_WORKER`

默认值：`STANDALONE_WORKER`

## port

服务监听端口。

类型：int

默认值：8102

## storage-path

文件的存储路径。

类型：Path

默认值：`${user.home}/jifa-storage`

## database-host

数据库的地址，对于非 `STANDALONE_WORKER` 角色，必须设置。目前仅支持 MySQL。

类型：String

默认值：从环境变量 `MYSQL_HOST` 中读取，若未设置则为空。

## database-name

数据库的名称，对于非 `STANDALONE_WORKER` 角色，必须设置。

类型：String

默认值：从环境变量 `MYSQL_DATABASE` 中读取，若未设置则为 `jifa`。

## database-user

数据库的用户，对于非 `STANDALONE_WORKER` 角色，必须设置。

类型：String

默认值：从环境变量 `MYSQL_USER` 中读取，若未设置则为 `jifa`。

## database-password

数据库的密码，对于非 `STANDALONE_WORKER` 角色，必须设置。

类型：String

默认值：从环境变量 `MYSQL_PASSWORD` 中读取，若未设置则为 `jifa`。

## scheduling-strategy

调度策略。

类型：Enum

- `ELASTIC`
- `STATIC`

默认值：null

## storage-pvc-name

弹性集群中使用的 PVC。

类型：String

默认值：null

## worker-image

弹性集群中用于运行 `WORKER` 节点的镜像。

类型：String

默认值：null

## elastic-worker-port

弹性集群中 `WORKER` 节点的服务监听端口。

类型：int

默认值：8102

## elastic-worker-idle-threshold

弹性集群中 `WORKER` 节点的空闲时间阈值，单位为分钟，最小值为 2。当一个 WORKER 节点的空闲时间超过此值时，将会自动停止。

类型：int

默认值：5

## allow-anonymous-access

是否允许匿名用户访问。

类型：boolean

默认值：ture

## allow-registration

是否允许注册新用户。

类型：boolean

默认值：true

## root-username

root 用户名。

类型：String

默认值：root

## root-password

root 用户密码。

类型：String

默认值：password

## input-files

本地的待分析文件，仅在 `STANDALONE_WORKER` 角色中使用。

类型：Path[]

默认值：null
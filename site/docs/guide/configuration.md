# Configuration

This page outlines the configurations supported by Jifa Server.

The default value of each configuration is provided
by [Configuration.java](https://github.com/eclipse/jifa/blob/main/server/src/main/java/org/eclipse/jifa/server/Configuration.java)
or the default configuration
file [application.yml](https://github.com/eclipse/jifa/blob/main/server/src/main/resources/application.yml).

Configure by adding application arguments `--jifa.<key>=<value>` or specifying a configuration file.

## role

The running role. Refer to [deployment](./deployment) for more information.

Type: Enum

- `STANDALONE_WORKER`
- `MASTER`
- `ELASTIC_WORKER`
- `STATIC_WORKER`

Default: `STANDALONE_WORKER`

## port

The service port.

Type: int

Default: 8102

## storage-path

The storage path.

Type: Path

Default: `${user.home}/jifa-storage`

## database-host

Database address, required if the role is not `STANDALONE_WORKER`. Currently only MySQL is supported.

Type：String

Default：Read from the environment variable `MYSQL_HOST`, and empty string is used if it's not set.

## database-name

Database name, required if the role is not `STANDALONE_WORKER`.

Type：String

Default：Read from the environment variable `MYSQL_DATABASE`, and `jifa` is used if it's not set.

## database-user

Database username, required if the role is not `STANDALONE_WORKER`.

Type：String

Default：Read from the environment variable `MYSQL_USER`, and `jifa` is used if it's not set.

## database-password

Database password, required if the role is not `STANDALONE_WORKER`.

Type：String

Default：Read from the environment variable `MYSQL_PASSWORD`, and `jifa` is used if it's not set.

## scheduling-strategy

The scheduling strategy. 

Type: Enum

- `STATIC`
- `ELASTIC`
 
Default: null

## storage-pvc-name

The PVC used in an elastic cluster.

Type: String

Default: null

## worker-image

Docker image used in an elastic cluster to run `WORKER` nodes.

Type: String

Default: null

## worker-jvm-options

JVM options used by `WORKER` nodes in an elastic cluster

Type：String

Default：null

## elastic-worker-port

The service port of `WORKER` nodes in an elastic cluster.

Type: int

Default: 8102

## elastic-worker-idle-threshold

The idle threshold of `WORKER` nodes in an elastic cluster, in minutes, with a minimum value of 2. When a `WORKER` node is idle for more than this threshold, it will automatically stop.

Type: int 

Default: 5

## allow-anonymous-access

Whether to allow anonymous user access.

Type: boolean

Default: true

## allow-registration

Whether to allow the registration of new users.

Type: boolean

Default: true

## root-username

Root username.

Type: String

Default: root

## root-password

Root password.

Type: String

Default: password

## input-files

Local files to be analyzed, used only in `STANDALONE_WORKER` role.

Type: Path[]

Default: null
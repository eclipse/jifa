# Deployment

## Standalone

Jifa will run in standalone mode if [role](./configuration#role) is `STANDALONE_WORKER`.

Suitable scenarios:

- Analyze local files.
- Deploy in a high-spec server.

In this mode, H2 will be used as the database by default.

### Steps

1. After [build](./getting-started#build), transfer `jifa.zip` (or `jifa.tar`) to your target environment.
2. `unzip jifa.zip` or `tar -xf jifa.tar`
3. `./jifa/bin/jifa`

## Cluster

In cluster mode, users need to prepare a MySQL database.

There are two types of roles:

- `MASTER`: is responsible for receiving analysis requests and forwarding to worker nodes.
- `STATIC_WORKER`: is responsible for processing analysis requests.
- `ELASTIC_WORKER`: is responsible for processing analysis requests.

### Deploy a Cluster

Refer to [cluster.yml](https://github.com/eclipse/jifa/blob/main/cluster.yml).

Command: `kubectl apply -f cluster.yml`

### Deploy a Static Cluster (only contains master nodes and static worker nodes)

The first two steps are the same as standalone mode.

- Launch `MASTER` nodes

  ```shell
  # set database
  $ export MYSQL_HOST=<host>
  $ export MYSQL_DATABASE=<name>
  $ export MYSQL_USER=<username>
  $ export MYSQL_PASSWORD=<password>

  $ ./jifa/bin/jifa --jifa.role=master
  ```

- Launch `STATIC_WORKER` nodes

  ```shell
  # set database
  $ export MYSQL_HOST=<host>
  $ export MYSQL_DATABASE=<name>
  $ export MYSQL_USER=<username>
  $ export MYSQL_PASSWORD=<password>

  $ ./jifa/bin/jifa --jifa.role=static_worker
  ```
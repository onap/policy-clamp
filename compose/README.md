# Policy Framework Docker Compose

The PF docker compose starts a small instance of docker containers for PF components.

## Features

- Starts all components, including Prometheus/Grafana dashboard
- Can start specific components
- Expose fixed ports so all the REST endpoints can be called with localhost:component_port

## Tech

Things to be installed beforehand:

- Linux VM if using Windows
- Docker
- Docker compose
- Any editor

## Installation

Assuming the docker repository has been cloned and workdir is ../docker/compose

- Install all PF components
```sh
./start-compose.sh
```

- Install an specific PF component
(accepted options: api pap apex-pdp distribution drools-pdp drools-apps xacml-pdp
policy-clamp-runtime-acm)


```sh
./start-compose.sh component

# that will start apex-pdp and its dependencies (pap, api, db, simulator)
./start-compose.sh apex-pdp
```

- Install an specific PF component with Grafana dashboard
(accepted options: api pap apex-pdp distribution drools-pdp drools-apps xacml-pdp
policy-clamp-runtime-acm)


```sh
./start-compose.sh component --grafana

# that will start apex-pdp and its dependencies (pap, api, db, simulator) + grafana and prometheus server
./start-compose.sh apex-pdp --grafana
```

## Docker image download localization

The docker images are always downloaded from nexus repository, but if needed to build a local
image, do an export ``export USE_LOCAL_IMAGES=true`` or edit the image tag in the docker compose
file. That will ensure that the newly built images locally are being used by not requesting a
download from nexus and using the image tagged as latest.

> When using the export command way, keep in mind that all policy images will need to be available
> locally.


## Docker image versions

The start-compose script is always looking for the latest SNAPSHOT version available (will
look locally first, then download from nexus if not available).
Note: if latest Policy-API docker image is 2.8-SNAPSHOT-latest, but on nexus it was released
2 days ago and in local environment it's 3 months old - it will use the 3 months old image,
so it's recommended to keep an eye on it.

If needed, the version can be edited on any docker compose yml file.

i.e: need to change db-migrator version
from compose.{database}.yml:
``image: ${CONTAINER_LOCATION}onap/policy-db-migrator:${POLICY_DOCKER_VERSION}``

replace the ${POLICY_DOCKER_VERSION} for the specific version needed


## Logs

Use ``docker compose logs`` or `docker logs ${container_name}` instructions on how to collect logs.

## Uninstall

Simply run the ``stop-compose.sh`` script. This will also generate logs from the services started
with compose.

```sh
./stop-compose.sh
```

## Database support

From Paris version onwards, this docker compose setup uses Postgres database; MariaDB support has
been removed.


### Docker compose files

To make it easier and clear how the docker compose system works, there are three files describing
the services:
- compose.common.yml
  - Simulator service
  - ACM-R Participants that don't connect directly to database
  - Messaging services (kafka, zookeeper)
  - Metrics services (prometheus, grafana, jaeger)
- compose.postgres.yml
  - Postgres database and policy-db-migrator working towards it
- compose.yml
  - All the policy components.

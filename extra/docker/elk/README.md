CLAMP Dashboard
===============

ELK stack for CLAMP : Logstash is used to retrieve events and notifications from DMaaP and pushes them into Elasticsearch.
Kibana is then used to extract statistics.


Deployment instructions
-----------------------

Requirements: docker-compose

1. Update configuration in docker-compose file
2. `docker-compose up -d elasticsearch logstash kibana`

If you encounter connection problems with kibana, first deploy elasticsearch, wait for it to be available, then kibana.

Backup/restore
--------------

backup.py and restore.py scripts are available inside the kibana docker image for saving and restoring the configuration.

### backup.py 
```
docker-compose exec kibana backup.py -C /saved-objects/
```
```
usage: backup.py [-h] [-v] [-C CONFIGURATION_PATH] [-f] [-H KIBANA_HOST]

Description of the script

optional arguments:
  -h, --help            show this help message and exit
  -v, --verbose         Use verbose logging
  -C CONFIGURATION_PATH, --configuration_path CONFIGURATION_PATH
                        Path of the configuration to be backed up.
  -f, --force           If the save folder already exists, overwrite files
                        matching a configuration item that should be written.
                        Files already in the folder that do not match are left
                        as-is.
  -H KIBANA_HOST, --kibana-host KIBANA_HOST
                        Kibana endpoint.

```

### restore.py
```
docker-compose exec kibana restore.py -C /saved-objects/ -f
```
```
usage: restore.py [-h] [-v] [-C CONFIGURATION_PATH] [-H KIBANA_HOST] [-f]

Restores the kibana configuration.

optional arguments:
  -h, --help            show this help message and exit
  -v, --verbose         Use verbose logging
  -C CONFIGURATION_PATH, --configuration_path CONFIGURATION_PATH
                        Path of the configuration to be restored.Should
                        contain at least one folder named index-
                        pattern,config,search,visualization or dashboard
  -H KIBANA_HOST, --kibana-host KIBANA_HOST
                        Kibana endpoint.
  -f, --force           Overwrite configuration if needed.
```

Tools
-----

The following tools are available in the 'tools/' folder.


### EsAutoQuery

Small script ease Elasticsearch /painless/ field development.
It reads a json file as a query for Elasticsearch, pushes it on the ES server, and display back the answer in a loop, each time the file is modified.


### DMaaP Service Mocker

Script that simulates control loop DMaaP services to provide sample data to logstash through DMaaP.

TODO
----
* Add a script that verifies that elasticsearch is available before starting loading the default configuration for kibana

CLAMP Dashboard
===============

ELK stack for CLAMP : Logstash is used to retrieve events and notifications from DMaaP and pushes them into Elasticsearch.
Kibana is then used to extract statistics.


Deployment instructions
-----------------------

Requirements: docker-compose

1. Update configuration in docker-compose file
2. `docker-compose up -d logstash kibana`
3. `docker-compose run default`  # loads the dashboard configuration for Kibana


Tools
-----

The following tools are available in the 'tools/' folder.


### EsAutoQuery

Small script ease Elasticsearch /painless/ field development.
It reads a json file as a query for Elasticsearch, pushes it on the ES server, and display back the answer in a loop, each time the file is modified.


### DMaaP Service Mocker

Script that simulates control loop DMaaP services to provide sample data to logstash through DMaaP.

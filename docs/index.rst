.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (c) 2017-2018 AT&T Intellectual Property. All rights reserved.

CLAMP - Closed Loop Automation Management Platform
==================================================
.. High level architecture, design, and packaging information for release planning and delivery.

.. include:: architecture.rst


Offered APIs
------------
CLAMP offers the following API:
* HealthCheck

.. line-block::

   URL: http://<host>:8080/restservices/clds/v1/clds/healthcheck
   Result: if in good health it will return OK: "HTTP/1.1 200", and the following json string content:

.. code-block:: json

    {
        "healthCheckComponent": "CLDS-APP",
        "healthCheckStatus": "UP",
        "description": "OK"
    }


Consumed APIs
-------------
CLAMP uses the API's exposed by the following ONAP components:

- SDC : REST based interface exposed by the SDC, Distribution of service to DCAE
- DCAE: REST based interface exposed by DCAE, Common Controller Framework, DCAE microservices onboarded (TCA, Stringmatch, Holmes (optional))
- Policy: REST based interface (the Policy team provide a "jar" to handle the communication), both XACML and Drools PDP, APIs to App-C/VF-C/SDN-C


Delivery
--------
CLAMP component is composed of a UI layer and a BackEnd layer and packaged into a single container.
CLAMP also requires a database instance with 1 DB, it uses MariaDB.
CLAMP also uses an ELK stack (Elastic Search, Logstash and Kibana) for the Dashboard.

.. blockdiag::


   blockdiag layers {
       orientation = portrait
       CLAMP_UI -> CLAMP_BACKEND;
       CLAMP_BACKEND -> CLDSDB;
       CLAMP_KIBANA -> CLAMP_ELASTICSEARCH;
       CLAMP_LOGSTASH -> CLAMP_ELASTICSEARCH;
       group l1 {
       color = blue;
       label = "CLAMP container";
       CLAMP_UI; CLAMP_BACKEND;
       }
       group l3 {
       color = orange;
       label = "MariaDB container";
       CLDSDB;
       }
       group l4 {
       color = green;
       label = "E_Search container";
       CLAMP_ELASTICSEARCH;
       }
       group l5 {
          color = green;
          label = "Kibana container";
          CLAMP_KIBANA;
       }
       group l6 {
          color = green;
          label = "LogStash container";
          CLAMP_LOGSTASH;
       }
   }

Logging & Diagnostic Information
--------------------------------
CLAMP uses logback framework to generate logs. The logback.xml file can be found under the [src/main/resources/ folder](src/main/resources).

With the default log settings, all logs will be generated into console and into root.log file under the CLAMP root folder. The root.log file is not allowed to be appended, thus restarting the CLAMP will result in cleaning of the old log files.


.. _index-clamp-installation:

Installation
------------
A [docker-compose example file](extra/docker/clamp/docker-compose.yml) can be found under the [extra/docker/clamp/ folder](extra/docker/).

Once the image has been built and is available locally, you can use the `docker-compose up` command to deploy a prepopullated database and a CLAMP instance available on [http://localhost:8080/designer/index.html](http://localhost:8080/designer/index.html).

Configuration
-------------
.. Where are they provided?
.. What are parameters and values?


Currently, the CLAMP docker image can be deployed with small configuration needs. Though, you might need to make small adjustments to the configuration. As CLAMP is spring based, you can use the SPRING_APPLICATION_JSON environment variable to update its parameters.

.. TODO detail config parameters and the usage


There are two needed datasource for Clamp. By default, both will try to connect to the localhost server using the credentials available in the example SQL files. If you need to change the default database host and/or credentials, you can do it by using the following json as SPRING_APPLICATION_JSON environment variable :

.. code-block:: json

    {
        "spring.datasource.cldsdb.url": "jdbc:mariadb://anotherDB.onap.org:3306/cldsdb4?verifyServerCertificate=false&useSSL=false&requireSSL=false&autoReconnect=true",
        "spring.datasource.cldsdb.username": "admin",
        "spring.datasource.cldsdb.password": "password",

        "clamp.config.sdc.catalog.url": "https://sdchost:8443/sdc/v1/catalog/",
        "clamp.config.sdc.hostUrl": "https://sdchost:8443/",
        "clamp.config.sdc.serviceUrl": "https://sdchost:8443/sdc/v1/catalog/services",
        "clamp.config.sdc.serviceUsername": "clamp",
        "clamp.config.sdc.servicePassword": "b7acccda32b98c5bb7acccda32b98c5b05D511BD6D93626E90D18E9D24D9B78CD34C7EE8012F0A189A28763E82271E50A5D4EC10C7D93E06E0A2D27CAE66B981",
        "clamp.config.dcae.inventory.url": "http://dcaegen2.host:8080",
        "clamp.config.dcae.dispatcher.url": "http://dcaegen2.host:8080",
        "clamp.config.policy.pdpUrl1": "https://policy-pdp.host:9091/pdp/ , testpdp, alpha123",
        "clamp.config.policy.pdpUrl2": "https://policy-pdp.host:9091/pdp/ , testpdp, alpha123",
        "clamp.config.policy.papUrl": "https://policy-pap.host:8443/pap/ , testpap, alpha123",
        "clamp.config.policy.clientKey": "5CE79532B3A2CB4D132FC0C04BF916A7"
        "clamp.config.files.sdcController":"file:/opt/clamp/config/sdc-controllers-config.json"
    }

SDC-Controllers config
----------------------
This file is a JSON that must be specified to Spring config, here is an example:

.. code-block:: json
	{
	  "sdc-connections":{
	    "sdc-controller1":{
	        "user": "User1",
	        "consumerGroup": "consumerGroup1",
	        "consumerId": "consumerId1",
	        "environmentName": "environmentName1",
	        "sdcAddress": "sdc.api.simpledemo.onap.org:8080",
	        "password": "bb3871669d893c7fb8aaacda31b77b4f537E67A081C2726889548ED7BC4C2DE6",
	        "pollingInterval":10,
	        "pollingTimeout":30,
	        "messageBusAddresses":["localhost"]
	    },
	     "sdc-controller2":{
	        "user": "User2",
	        "consumerGroup": "consumerGroup2",
	        "consumerId": "consumerId2",
	        "environmentName": "environmentName2",
	        "sdcAddress": "sdc.api.simpledemo.onap.org:8080",
	        "password": "bb3871669d893c7fb8aaacda31b77b4f537E67A081C2726889548ED7BC4C2DE6",
	        "pollingInterval":10,
	        "pollingTimeout":30,
	        "messageBusAddresses":["localhost"]
	    }
	  }
	}

Multiple controllers can be configured so that Clamp is able to receive the notifications from different SDC servers.
Each Clamp existing in a cluster should have different consumerGroup and consumerId so that they can each consume the SDC notification.
The environmentName is normally the Dmaap Topic used by SDC. 
If the sdcAddress is not specified or not available (connection failure) the messageBusAddresses will be used (Dmaap servers) 

Administration
--------------

A user can access CLAMP UI at the following URL : http://localhost:8080/designer/index.html.
(in this URL 'localhost' must be replaced by the actual host where CLAMP has been installed if it is not your current localhost)

.. code-block:: html

    Default username : admin
    Default password : password

Human Interfaces
----------------
.. Basic info on the interface type, ports/protocols provided over, etc.

User Interface (CLAMP Designer) - serve to configure control loop
The following actions are done using the UI:

* Design a control loop flow by selecting a predefined template from a list
  (a template is an orchestration chain of Micro-services, so the template
  defines how the micro-services of the control loop are chained together)

* Give value to the configuration the parameters of each micro-service of
  the control loop

* Select the service and VNF(of that service) to which the control loop
  will be attached

* Configure the operational policy(the actual operation resulting from
  the control loop)

* Generate the “TOSCA” blueprint that will be used by DCAE to start the
  control loop (The blueprint will be sent first to SDC and SDC will
  publish it to DCAE)

* Trigger the deployment of the Control loop in DCAE

* Control (start/stop) the operation of the control loop in DCAE



HealthCheck API - serve to verify CLAMP status (see offered API's section)

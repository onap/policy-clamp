.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright © 2017 AT&T Intellectual Property. All rights reserved.

CLAMP - Closed Loop Automation Management Platform
==================================================
.. High level architecture, design, and packaging information for release planning and delivery.

CLAMP is a platform for designing and managing control loops. It is used to design a closed loop, configure it with specific parameters for a particular network service, then deploying and undeploying it.  Once deployed, the user can also update the loop with new parameters during runtime, as well as suspending and restarting it.

It interacts with other systems to deploy and execute the closed loop. For example, it pushes the control loop design to the SDC catalog, associating it with the VF resource.  It requests from DCAE the instantiation of microservices to manage the closed loop flow.  Further, it creates and updates multiple policies in the Policy Engine that define the closed loop flow.

The ONAP CLAMP platform abstracts the details of these systems under the concept of a control loop model.  The design of a control loop and its management is represented by a workflow in which all relevant system interactions take place.  This is essential for a self-service model of creating and managing control loops, where no low-level user interaction with other components is required.

At a higher level, CLAMP is about supporting and managing the broad operational life cycle of VNFs/VMs and ultimately ONAP components itself. It will offer the ability to design, test, deploy and update control loop automation - both closed and open. Automating these functions would represent a significant saving on operational costs compared to traditional methods.



.. toctree::
    :maxdepth: 1


Delivery
--------
CLAMP component is composed of a UI layer and a BackEND layer and packaged into a single container.
CLAMP also requires a database instance with 2 DB, it uses MariaDB.

.. blockdiag::


   blockdiag layers {
   orientation = portrait
   CLAMP_UI -> CLAMP_BACKEND;
   CLAMP_BACKEND -> CAMUNDADB;
   CLAMP_BACKEND -> CLDSDB;
   group l1 {
   color = blue;
   label = "CLAMP container";
   CLAMP_UI; CLAMP_BACKEND;
   }
   group l3 {
   color = orange;
   label = "MariaDB container";
   CAMUNDADB; CLDSDB;
   }
   }


Logging & Diagnostic Information
--------------------------------
Clamp uses logback framework to generate logs. The logback.xml file cand be found under the [src/main/resources/ folder](src/main/resources).

With the default log settings, all logs will be generated into console and into root.log file under the Clamp root folder. The root.log file is not allowed to be appended, thus restarting the clamp will result in cleaning of the old log files.



Installation
------------
A [docker-compose example file](extra/docker/clamp/docker-compose.yml) can be found under the [extra/docker/clamp/ folder](extra/docker/).

Once the image has been built and is available locally, you can use the `docker-compose up` command to deploy a prepopullated database and a clamp instance available on [http://localhost:8080/designer/index.html](http://localhost:8080/designer/index.html).

Configuration
-------------
.. Where are they provided?
.. What are parameters and values?


Currently, the clamp docker image can be deployed with small configuration needs. Though, you might need to make small adjustments to the configuration. As clamp is spring based, you can use the SPRING_APPLICATION_JSON environment variable to update its parameters.

.. TODO detail config parameters and the usage


There are two needed datasource for Clamp. By default, both will try to connect to the localhost server using the credentials available in the example SQL files. If you need to change the default database host and/or credentials, you can do it by using the following json as SPRING_APPLICATION_JSON environment variable :

.. code-block:: json

    {
        "spring.datasource.camunda.url": "jdbc:mariadb://anotherDB.onap.org:3306/camundabpm?verifyServerCertificate=false&useSSL=false&requireSSL=false&autoReconnect=true",
        "spring.datasource.camunda.username": "admin",
        "spring.datasource.camunda.password": "password",
        "spring.datasource.cldsdb.url": "jdbc:mariadb://anotherDB.onap.org:3306/cldsdb4?verifyServerCertificate=false&useSSL=false&requireSSL=false&autoReconnect=true",
        "spring.datasource.cldsdb.username": "admin",
        "spring.datasource.cldsdb.password": "password"
    }

OR

.. code-block:: json

    {
        "spring":
        {
            "datasource":
            {
                "camunda":
                {
                    "url": "jdbc:mariadb://anotherDB.onap.org:3306/camundabpm?verifyServerCertificate=false&useSSL=false&requireSSL=false&autoReconnect=true",
                    "username": "admin",
                    "password": "password"
                },

                "cldsdb":
                {
                "url": "jdbc:mariadb://anotherDB.onap.org:3306/cldsdb4?verifyServerCertificate=false&useSSL=false&requireSSL=false&autoReconnect=true",
                "username": "admin",
                "password": "password"
                }
            }
        }
    }

Administration
--------------

A user can access CLAMP UI at the following URL : http://localhost:8080/designer/index.html.

Default username : admin
Default password : password


Human Interfaces
----------------
.. Basic info on the interface type, ports/protocols provided over, etc.

User Interface (CLAMP Designer) - serve to configure control loop

HealthCheck API - serve to verify CLAMP status
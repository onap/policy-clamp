# Summary

CLAMP is a platform for designing and managing control loops. It is used to design a closed loop, configure it with specific parameters for a particular network service, then deploying and undeploying it.  Once deployed, the user can also update the loop with new parameters during runtime, as well as suspending and restarting it.

It interacts with other systems to deploy and execute the closed loop. For example, it pushes the control loop design to the SDC catalog, associating it with the VF resource.  It requests from DCAE the instantiation of microservices to manage the closed loop flow.  Further, it creates and updates multiple policies in the Policy Engine that define the closed loop flow.

The ONAP CLAMP platform abstracts the details of these systems under the concept of a control loop model.  The design of a control loop and its management is represented by a workflow in which all relevant system interactions take place.  This is essential for a self-service model of creating and managing control loops, where no low-level user interaction with other components is required.

At a higher level, CLAMP is about supporting and managing the broad operational life cycle of VNFs/VMs and ultimately ONAP components itself. It will offer the ability to design, test, deploy and update control loop automation - both closed and open. Automating these functions would represent a significant saving on operational costs compared to traditional methods.

# Developer Contact
Owner: ONAP CLAMP Dev team
Mailing List : onap-discuss@lists.onap.org
Add the following prefix to Subject on the mailing list : [CLAMP]
See here to subscribe : https://wiki.onap.org/display/DW/Mailing+Lists

# Wiki
https://wiki.onap.org/display/DW/CLAMP+Project

# Build
Jenkins Job: ${jenkins-joblink}

CLAMP UI: ${cockpit-link}

Logs: ${elk-link}

# Docker image

## Building 
You can use the following command to build the clamp docker image:
```
mvn clean install -P docker
```

## Deployment
Currently, the clamp docker image can be deployed with small configuration needs. Though, you might need to make small adjustments to the configuration. As clamp is spring based, you can use the SPRING_APPLICATION_JSON environment variable to update its parameters. 

### Databases
There are two needed datasource for Clamp. By default, both will try to connect to the localhost server using the credentials available in the example SQL files. If you need to change the default database host and/or credentials, you can do it by using the following json as SPRING_APPLICATION_JSON environment variable :

```json
{
    "spring.datasource.camunda.url": "jdbc:mysql://anotherDB.onap.org:3306/camundabpm?verifyServerCertificate=false&useSSL=false&requireSSL=false&autoReconnect=true",
    "spring.datasource.camunda.username": "admin",
    "spring.datasource.camunda.password": "password",
    "spring.datasource.cldsdb.url": "jdbc:mysql://anotherDB.onap.org:3306/cldsdb4?verifyServerCertificate=false&useSSL=false&requireSSL=false&autoReconnect=true",
    "spring.datasource.cldsdb.username": "admin",
    "spring.datasource.cldsdb.password": "password"
}
```

OR 

```json
{
    "spring": 
    {
        "datasource": 
        {
            "camunda": 
            {
                "url": "jdbc:mysql://anotherDB.onap.org:3306/camundabpm?verifyServerCertificate=false&useSSL=false&requireSSL=false&autoReconnect=true",
                "username": "admin",
                "password": "password"
            },

            "cldsdb": 
            {
                "url": "jdbc:mysql://anotherDB.onap.org:3306/cldsdb4?verifyServerCertificate=false&useSSL=false&requireSSL=false&autoReconnect=true",
                "username": "admin",
                "password": "password"
            }
        }
    }
}

```

### Docker-compose

A [docker-compose example file](extra/docker/clamp/docker-compose.yml) can be found under the [extra/docker/clamp/ folder](extra/docker/).

Once the image has been built and is available locally, you can use the `docker-compose up` command to deploy a prepopullated database and a clamp instance available on [http://localhost:8080/designer/index.html](http://localhost:8080/designer/index.html).


### Logs

Clamp uses logback framework to generate logs. The logback.xml file cand be found under the [src/main/resources/ folder](src/main/resources). 

With the default log settings, all logs will be generated into console and into root.log file under the Clamp root folder. The root.log file is not allowed to be appended, thus restarting the clamp will result in cleaning of the old log files.

### Api

You can see the swagger definition for the jaxrs apis at `/restservices/clds/v1/openapi.json`
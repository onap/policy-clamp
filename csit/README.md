# Running Policy Framework CSIT

## Using Docker Compose environment

Policy Framework Continuous System and Integration Tests are executed daily on jenkins jobs
targeting master branch. The runs are available at https://jenkins.onap.org/view/policy/

The CSIT suites are also used by developers as another guarantee that new code or changes
delivered on main code do not affect the expected behaviour for the already delivered
functionalities or new tests are added when a new functionality is added.

To execute the tests on a local environment, the steps are the following:

> all the instructions assume docker repository was cloned to /git/policy/docker

- after cloning the project, go to ../docker/csit
- to run a test, execute the run-project-csit.sh script

`./run-project-csit.sh <policy-component>`

The options for <policy-component> are:
- api
- pap
- apex-pdp
- clamp (for runtime-acm and participants)
- drools-pdp
- drools-applications
- xacml-pdp
- distribution
- opa-pdp

The command above with download the latest SNAPSHOT version available for the policy-component
being tested. Version is collected from [PF Release Data](https://github.com/onap/policy-parent/blob/master/integration/src/main/resources/release/pf_release_data.csv)

To start the containers with images generated in local environment, the script can be run with the
flag `--local`

`./run-project-csit.sh api --local`

The command above with start the docker containers for `policy-api` and `policy-db-migrator` using
the latest image created at the local environment. When using the flag `--local` it will look for
all the policy components needed for the test suites to be executed. The support services like
PostgreSQL, Kafka, Prometheus, Grafana will always be downloaded if not present.

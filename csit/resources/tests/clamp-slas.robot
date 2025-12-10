*** Settings ***
Name        Service Level Agreement Suite
Library     Collections
Library     RequestsLibrary
Library     OperatingSystem
Library     json
Resource    common-library.robot

*** Keywords ***
ValidateResponseTimeForClamp
    [Arguments]  ${uri}  ${method}
    [Documentation]  Check if uri response is under the 1500ms required time for clamp metrics
    ValidateResponseTime  acm-metrics  ${uri}  ${method}  1500

*** Test Cases ***
WaitForPrometheusServer
    [Documentation]  Sleep time to wait for Prometheus server to gather all metrics
    Sleep    1 minute

ValidateResponseTimeForHealthcheck
    [Documentation]  Validate component healthcheck response time
    ValidateResponseTimeForClamp  /health  GET

ValidateResponseTimeQueryAcDefinition
    [Documentation]  Validate query AC Definitions response time
    ValidateResponseTimeForClamp  /v2/compositions/{compositionId}  GET

ValidateResponseTimeQueryAcInstance
    [Documentation]  Validate query AC instance response time
    ValidateResponseTimeForClamp  /v2/compositions/{compositionId}/instances/{instanceId}  GET

ValidateResponseTimeQueryAcInstances
    [Documentation]  Validate query all AC instances response time
    ValidateResponseTimeForClamp  /v2/compositions/{compositionId}/instances  GET

ValidateResponseTimeStateChange
    [Documentation]  Validate AC instance StateChange response time
    ValidateResponseTimeForClamp  /v2/compositions/{compositionId}/instances/{instanceId}  PUT

ValidateResponseTimeCallParticipants
    [Documentation]  Validate call AC participants response time
    ValidateResponseTimeForClamp  /v2/participants  PUT

ValidateResponseTimeCommissioning
    [Documentation]  Validate commission AC Definitions response time
    ValidateResponseTimeForClamp  /v2/compositions  POST

ValidateResponseTimeInstantiation
    [Documentation]  Validate create AC Instance response time
    ValidateResponseTimeForClamp  /v2/compositions/{compositionId}/instances  POST

ValidateResponseTimeDeleteInstance
    [Documentation]  Validate delete AC Instance response time
    ValidateResponseTimeForClamp  /v2/compositions/{compositionId}/instances/{instanceId}  DELETE

ValidateResponseTimeDeleteDefinition
    [Documentation]  Validate delete AC Definition response time
    ValidateResponseTimeForClamp  /v2/compositions/{compositionId}  DELETE
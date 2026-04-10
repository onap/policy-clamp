*** Settings ***
Library    Collections
Library    RequestsLibrary
Library    OperatingSystem
Library    json
Library    Process

*** Keywords ***

PerformPatchRequest
    [Arguments]  ${domain}  ${url}  ${expectedstatus}  ${patchjson}  ${params}  ${auth}
    Log  Creating session http://${domain}
    ${session}=  Create Session  policy  http://${domain}  auth=${auth}
    ${headers}=  Create Dictionary  Accept=application/json  Content-Type=application/json
    ${resp}=  PATCH On Session  policy  ${url}  data=${patchjson}  params=${params}  headers=${headers}  expected_status=${expectedstatus}
    Log  Received response from policy ${resp.text}
    RETURN  ${resp}

PerformPostRequest
    [Arguments]  ${domain}  ${url}  ${expectedstatus}  ${postjson}  ${params}  ${auth}
    Log  Creating session http://${domain}
    ${session}=  Create Session  policy  http://${domain}  auth=${auth}
    ${headers}=  Create Dictionary  Accept=application/json  Content-Type=application/json
    ${resp}=  POST On Session  policy  ${url}  data=${postjson}  params=${params}  headers=${headers}  expected_status=${expectedstatus}
    Log  Received response from policy ${resp.text}
    RETURN  ${resp}

PerformPostRequestWithYaml
    [Arguments]  ${domain}  ${url}  ${expectedstatus}  ${postyaml}  ${params}  ${auth}
    Log  Creating session http://${domain}
    ${session}=  Create Session  policy  http://${domain}  auth=${auth}
    ${headers}=  Create Dictionary  Accept=application/yaml  Content-Type=application/yaml
    ${resp}=  POST On Session  policy  ${url}  data=${postyaml}  params=${params}  headers=${headers}  expected_status=${expectedstatus}
    Log  Received response from policy ${resp.text}
    RETURN  ${resp}

PerformPutRequest
    [Arguments]  ${domain}  ${url}  ${expectedstatus}  ${params}  ${auth}
    Log  Creating session http://${domain}
    ${session}=  Create Session  policy  http://${domain}  auth=${auth}
    ${headers}=  Create Dictionary  Accept=application/json  Content-Type=application/json
    ${resp}=  PUT On Session  policy  ${url}  params=${params}  headers=${headers}  expected_status=${expectedstatus}
    Log  Received response from policy ${resp.text}
    RETURN  ${resp}

PerformGetRequest
    [Arguments]  ${domain}  ${url}  ${expectedstatus}  ${params}  ${auth}
    Log  Creating session http://${domain}
    ${session}=  Create Session  policy  http://${domain}  auth=${auth}
    ${headers}=  Create Dictionary  Accept=application/json  Content-Type=application/json
    ${resp}=  GET On Session  policy  ${url}  params=${params}  headers=${headers}  expected_status=${expectedstatus}
    Log  Received response from policy ${resp.text}
    RETURN  ${resp}

PerformDeleteRequest
    [Arguments]  ${domain}  ${url}  ${expectedstatus}  ${auth}
    Log  Creating session http://${domain}
    ${session}=  Create Session  policy  http://${domain}  auth=${auth}
    ${headers}=  Create Dictionary  Accept=application/json  Content-Type=application/json
    ${resp}=  DELETE On Session  policy  ${url}  headers=${headers}  expected_status=${expectedstatus}
    Log  Received response from policy ${resp.text}

GetMetrics
    [Arguments]  ${domain}  ${auth}  ${context_path}
    Log  Creating session http://${domain}
    ${session}=  Create Session  policy  http://${domain}  auth=${auth}
    ${resp}=  GET On Session  policy  ${context_path}metrics  expected_status=200
    Log  Received response from policy ${resp.text}
    RETURN  ${resp}

VerifyTracingWorks
    [Arguments]  ${domain}    ${service}
    Log  Creating session http://${domain}
    ${session}=  Create Session  jaeger  http://${domain}
    ${resp}=  GET On Session  jaeger  /api/traces  params=service=${service}    expected_status=200
    Log  Received response from jaeger ${resp.text}
    RETURN  ${resp}

QueryPrometheus
    [Arguments]  ${query}
    ${params}=  Create Dictionary  query=${query}
    ${resp}=  GET  http://${PROMETHEUS_IP}/api/v1/query  ${params}
    Status Should Be    OK
    Log  Received response from Prometheus ${resp.text}
    RETURN  ${resp.json()}

ValidateResponseTime
    [Arguments]  ${job}  ${uri}  ${method}  ${timeLimit}
    [Documentation]  Check if uri response is under the required time
    ${resp}=  QueryPrometheus  http_server_requests_seconds_sum{uri="${uri}",method="${method}",job="${job}"}/http_server_requests_seconds_count{uri="${uri}",method="${method}",job="${job}"}
    ${rawNumber}=  Evaluate  ${resp['data']['result'][0]['value'][1]}
    ${actualTime}=   Set Variable  ${rawNumber * ${1000}}
    Should Be True   ${actualTime} <= ${timeLimit}


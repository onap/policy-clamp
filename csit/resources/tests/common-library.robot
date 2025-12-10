*** Settings ***
Library    Collections
Library    RequestsLibrary
Library    OperatingSystem
Library    json
Library    Process

*** Keywords ***

PolicyAdminAuth
    ${policyadmin}=   Create list   policyadmin    zb!XztG34
    RETURN  ${policyadmin}

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

CreatePolicy
    [Arguments]  ${url}  ${expectedstatus}  ${postjson}  ${policyname}  ${policyversion}
    [Documentation]  Create the specific policy
    ${policyadmin}=  PolicyAdminAuth
    ${resp}=  PerformPostRequest  ${POLICY_API_IP}  ${url}  ${expectedstatus}  ${postjson}  null  ${policyadmin}

CreatePolicyWithYaml
    [Arguments]  ${url}  ${expectedstatus}  ${postyaml}
    [Documentation]  Create the specific policy
    ${policyadmin}=  PolicyAdminAuth
    ${resp}=  PerformPostRequestWithYaml  ${POLICY_API_IP}  ${url}  ${expectedstatus}  ${postyaml}  null  ${policyadmin}

CreateFailurePolicyWithYaml
    [Arguments]  ${url}  ${expectedstatus}  ${postyaml}  ${keyword}
    [Documentation]  Trying to create policy with Invalid Data
    ${policyadmin}=  PolicyAdminAuth
    ${resp}=  PerformPostRequestWithYaml  ${POLICY_API_IP}  ${url}  ${expectedstatus}  ${postyaml}  null  ${policyadmin}
    Should Contain    ${resp.text}    ${keyword}


CreatePolicySuccessfully
    [Arguments]  ${url}  ${postjson}  ${policyname}  ${policyversion}
    [Documentation]  Create the specific policy
    ${policyadmin}=  PolicyAdminAuth
    ${resp}=  PerformPostRequest  ${POLICY_API_IP}  ${url}  201  ${postjson}  null  ${policyadmin}
    Dictionary Should Contain Key  ${resp.json()['topology_template']['policies'][0]}  ${policyname}
    Should Be Equal As Strings  ${resp.json()['topology_template']['policies'][0]['${policyname}']['version']}  ${policyversion}

CreateNodeTemplate
    [Arguments]  ${url}  ${expectedstatus}  ${postjson}  ${nodeTemplateListLength}
    [Documentation]  Create the node templates
    ${policyadmin}=  PolicyAdminAuth
    ${resp}=  PerformPostRequest  ${POLICY_API_IP}  ${url}  ${expectedstatus}  ${postjson}  \  ${policyadmin}
    Run Keyword If  ${expectedstatus}==201  Length Should Be  ${resp.json()['topology_template']['node_templates']}  ${nodeTemplateListLength}


QueryPdpGroups
    [Documentation]    Verify pdp group query - suphosts upto 2 groups
    [Arguments]  ${groupsLength}  ${group1Name}  ${group1State}  ${policiesLengthInGroup1}  ${group2Name}  ${group2State}  ${policiesLengthInGroup2}
    ${policyadmin}=  PolicyAdminAuth
    ${resp}=  PerformGetRequest  ${POLICY_PAP_IP}  /policy/pap/v1/pdps  200  null  ${policyadmin}
    Length Should Be  ${resp.json()['groups']}  ${groupsLength}
    Should Be Equal As Strings  ${resp.json()['groups'][0]['name']}  ${group1Name}
    Should Be Equal As Strings  ${resp.json()['groups'][0]['pdpGroupState']}  ${group1State}
    Length Should Be  ${resp.json()['groups'][0]['pdpSubgroups'][0]['policies']}  ${policiesLengthInGroup1}
    Run Keyword If  ${groupsLength}>1  Should Be Equal As Strings  ${resp.json()['groups'][1]['name']}  ${group2Name}
    Run Keyword If  ${groupsLength}>1  Should Be Equal As Strings  ${resp.json()['groups'][1]['pdpGroupState']}  ${group2State}
    Run Keyword If  ${groupsLength}>1  Length Should Be  ${resp.json()['groups'][1]['pdpSubgroups'][0]['policies']}  ${policiesLengthInGroup2}

QueryPolicyAudit
    [Arguments]  ${url}  ${expectedstatus}  ${pdpGroup}  ${pdpType}  ${policyName}  ${expectedAction}
    ${policyadmin}=  PolicyAdminAuth
    ${resp}=  PerformGetRequest  ${POLICY_PAP_IP}  ${url}  ${expectedstatus}  recordCount=4   ${policyadmin}
    Log  Received response from queryPolicyAudit ${resp.text}
    FOR    ${responseEntry}    IN    @{resp.json()}
    Exit For Loop IF      '${responseEntry['policy']['name']}'=='${policyName}' and '${responseEntry['action']}'=='${expectedAction}'
    END
    Should Be Equal As Strings    ${responseEntry['pdpGroup']}  ${pdpGroup}
    Should Be Equal As Strings    ${responseEntry['pdpType']}  ${pdpType}
    Should Be Equal As Strings    ${responseEntry['policy']['name']}  ${policyName}
    Should Be Equal As Strings    ${responseEntry['policy']['version']}  1.0.0
    Should Be Equal As Strings    ${responseEntry['action']}  ${expectedAction}
    Should Be Equal As Strings    ${responseEntry['user']}  policyadmin

QueryPolicyStatus
    [Documentation]    Verify policy deployment status
    [Arguments]  ${policyName}  ${pdpGroup}  ${pdpType}  ${pdpName}  ${policyTypeName}
    ${policyadmin}=  PolicyAdminAuth
    ${resp}=  PerformGetRequest  ${POLICY_PAP_IP}  /policy/pap/v1/policies/status  200  null   ${policyadmin}
    FOR    ${responseEntry}    IN    @{resp.json()}
    Exit For Loop IF      '${responseEntry['policy']['name']}'=='${policyName}'
    END
    Should Be Equal As Strings    ${resp.status_code}     200
    Should Be Equal As Strings    ${responseEntry['pdpGroup']}  ${pdpGroup}
    Should Be Equal As Strings    ${responseEntry['pdpType']}  ${pdpType}
    Should Be Equal As Strings    ${responseEntry['policy']['name']}  ${policyName}
    Should Be Equal As Strings    ${responseEntry['policy']['version']}  1.0.0
    Should Be Equal As Strings    ${responseEntry['policyType']['name']}  ${policyTypeName}
    Should Be Equal As Strings    ${responseEntry['policyType']['version']}  1.0.0
    Should Be Equal As Strings    ${responseEntry['deploy']}  True
    Should Be Equal As Strings    ${responseEntry['state']}  SUCCESS

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

CheckKafkaTopic
    [Arguments]    ${topic}    ${expected_status}
    ${resp}=    Run Process    ${CURDIR}/kafka_consumer.py    ${topic}    60    ${expected_status}    ${KAFKA_IP}
    Log    ${resp.stdout}
    Should Contain    ${resp.stdout}    ${expected_status}
    RETURN    ${resp.stdout}

GetKafkaTopic
    [Arguments]    ${topic}
    ${resp}=    Run Process    ${CURDIR}/make_topics.py    ${topic}    ${KAFKA_IP}
    Log    ${resp.stdout}

ValidatePolicyExecution
    [Arguments]  ${url}  ${executionTime}
    [Documentation]  Check that policy execution under X milliseconds
    ${resp}=  QueryPrometheus  ${url}
    ${rawNumber}=  Evaluate  ${resp['data']['result'][0]['value'][1]}
    ${actualTime}=   Set Variable  ${rawNumber * ${1000}}
    Should Be True   ${actualTime} <= ${executionTime}

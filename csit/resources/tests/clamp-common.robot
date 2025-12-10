*** Settings ***
Library     Collections
Library     RequestsLibrary
Library     OperatingSystem
Library     String
Library     json
Library     yaml
Library    Process
Resource    common-library.robot

*** Keywords ***

ExecuteQuery
    [Arguments]  ${file}
    [Documentation]    Execute Query
    Run Keyword If    '${TEST_ENV}'=='k8s'    set Suite variable  ${executeQueryFile}  ${CURDIR}/data/script/execute-queryk8.sh
    ...    ELSE    set Suite variable  ${executeQueryFile}  ${CURDIR}/data/script/execute-query.sh
    ${result}=    Run Process    ${executeQueryFile}    ${file}
    Should Be Equal As Strings    ${result.rc}     0

VerifyHealthcheckApi
    [Documentation]    Verify Healthcheck on policy-api
    ${auth}=    PolicyAdminAuth
    ${resp}=    MakeGetRequest  policy  ${POLICY_API_IP}  /policy/api/v1/health  ${auth}
    Should Be Equal As Strings    ${resp.status_code}   200

VerifyHealthcheckPap
    [Documentation]    Verify Healthcheck on policy-pap
    ${auth}=    PolicyAdminAuth
    ${resp}=    MakeGetRequest  policy  ${POLICY_PAP_IP}  /policy/pap/v1/health  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200

VerifyPriming
    [Arguments]  ${theCompositionId}  ${primestate}
    [Documentation]    Verify the AC definitions are primed to the participants
    ${auth}=    ClampAuth
    ${resp}=    MakeGetRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${theCompositionId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}   200
    Should Be Equal As Strings  ${resp.json()['stateChangeResult']}  NO_ERROR
    Run Keyword If  ${resp.status_code}==200  Should Be Equal As Strings  ${resp.json()['state']}  ${primestate}

VerifyStateChangeResultPriming
    [Arguments]  ${theCompositionId}  ${stateChangeResult}
    [Documentation]    Verify the AC definitions are primed to the participants
    ${auth}=    ClampAuth
    ${resp}=    MakeGetRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${theCompositionId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}   200
    Run Keyword If  ${resp.status_code}==200  Should Be Equal As Strings  ${resp.json()['stateChangeResult']}  ${stateChangeResult}

VerifyDeployStatus
    [Arguments]  ${theCompositionId}  ${theInstanceId}  ${deploystate}
    [Documentation]  Verify the Deploy status of automation composition.
    ${auth}=    ClampAuth
    ${resp}=    MakeGetRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${theCompositionId}/instances/${theInstanceId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    Should Be Equal As Strings  ${resp.json()['stateChangeResult']}  NO_ERROR
    Run Keyword If  ${resp.status_code}==200  Should Be Equal As Strings  ${resp.json()['deployState']}  ${deploystate}

VerifySubStatus
    [Arguments]  ${theCompositionId}  ${theInstanceId}
    [Documentation]  Verify the Sub status of automation composition.
    ${auth}=    ClampAuth
    ${resp}=    MakeGetRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${theCompositionId}/instances/${theInstanceId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    Should Be Equal As Strings  ${resp.json()['stateChangeResult']}  NO_ERROR
    Run Keyword If  ${resp.status_code}==200  Should Be Equal As Strings  ${resp.json()['subState']}  NONE

VerifyStateChangeResult
    [Arguments]  ${theCompositionId}  ${theInstanceId}  ${stateChangeResult}
    [Documentation]  Verify the Deploy status of automation composition.
    ${auth}=    ClampAuth
    ${resp}=    MakeGetRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${theCompositionId}/instances/${theInstanceId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    Run Keyword If  ${resp.status_code}==200  Should Be Equal As Strings  ${resp.json()['stateChangeResult']}  ${stateChangeResult}

VerifyPropertiesUpdated
    [Arguments]  ${theCompositionId}  ${theInstanceId}  ${textToFind}
    [Documentation]  Verify the Deploy status of automation composition.
    ${auth}=    ClampAuth
    ${resp}=    MakeGetRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${theCompositionId}/instances/${theInstanceId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    ${respstring}   Convert To String   ${resp.json()}
    Run Keyword If  ${resp.status_code}==200  Should Match Regexp  ${respstring}  ${textToFind}

VerifyInternalStateElementsRuntime
    [Arguments]  ${theCompositionId}  ${theInstanceId}  ${deploystate}
    [Documentation]  Verify the Instance elements during operation
    ${auth}=    ClampAuth
    ${resp}=    MakeGetRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${theCompositionId}/instances/${theInstanceId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    Should Be Equal As Strings  ${resp.json()['deployState']}   ${deploystate}
    ${respstring}   Convert To String   ${resp.json()['elements']['709c62b3-8918-41b9-a747-d21eb80c6c34']['outProperties']['InternalState']}
    Should Be Equal As Strings  ${respstring}  ${deploystate}

VerifyMigratedElementsRuntime
    [Arguments]  ${theCompositionId}  ${theInstanceId}
    [Documentation]  Verify the Instance elements after Migration
    ${auth}=    ClampAuth
    ${resp}=    MakeGetRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${theCompositionId}/instances/${theInstanceId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    ${respstring}   Convert To String   ${resp.json()}
    Should Match Regexp  ${respstring}  Sim_NewAutomationCompositionElement
    Should Match Regexp  ${respstring}  Sim_NewAutomationCompositionElement2
    Should Not Match Regexp  ${respstring}  Sim_SinkAutomationCompositionElement
    ${respstring}   Convert To String   ${resp.json()['elements']['709c62b3-8918-41b9-a747-d21eb79c6c34']['outProperties']['stage']}
    Should Be Equal As Strings  ${respstring}  [1, 2]
    ${respstring}   Convert To String   ${resp.json()['elements']['709c62b3-8918-41b9-a747-d21eb79c6c35']['outProperties']['stage']}
    Should Be Equal As Strings  ${respstring}  [0, 1]
    ${respstring}   Convert To String   ${resp.json()['elements']['709c62b3-8918-41b9-a747-d21eb79c6c37']['outProperties']['stage']}
    Should Be Equal As Strings  ${respstring}  [0, 2]
    ${respstring}   Convert To String   ${resp.json()['elements']['709c62b3-8918-41b9-a747-d21eb79c6c40']['outProperties']['stage']}
    Should Be Equal As Strings  ${respstring}  [1, 2]

VerifyPrepareElementsRuntime
    [Arguments]  ${theCompositionId}  ${theInstanceId}
    [Documentation]  Verify the Instance elements after Prepare
    ${auth}=    ClampAuth
    ${resp}=    MakeGetRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${theCompositionId}/instances/${theInstanceId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    ${respstring}   Convert To String   ${resp.json()['elements']['709c62b3-8918-41b9-a747-d21eb79c6c34']['outProperties']['prepareStage']}
    Should Be Equal As Strings  ${respstring}  [1, 2]
    ${respstring}   Convert To String   ${resp.json()['elements']['709c62b3-8918-41b9-a747-d21eb79c6c35']['outProperties']['prepareStage']}
    Should Be Equal As Strings  ${respstring}  [0, 1]
    ${respstring}   Convert To String   ${resp.json()['elements']['709c62b3-8918-41b9-a747-d21eb79c6c36']['outProperties']['prepareStage']}
    Should Be Equal As Strings  ${respstring}  [0, 2]

VerifyRollbackElementsRuntime
    [Arguments]  ${theCompositionId}  ${theInstanceId}
    [Documentation]  Verify the Instance elements after Rollback
    ${auth}=    ClampAuth
    ${resp}=    MakeGetRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${theCompositionId}/instances/${theInstanceId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    ${respstring}   Convert To String   ${resp.json()}
    Should Not Match Regexp  ${respstring}  Sim_NewAutomationCompositionElement
    Should Match Regexp  ${respstring}  Sim_SinkAutomationCompositionElement
    ${respstring}   Convert To String   ${resp.json()['elements']['709c62b3-8918-41b9-a747-d21eb79c6c34']['outProperties']['rollbackStage']}
    Should Be Equal As Strings  ${respstring}  [2, 1]
    ${respstring}   Convert To String   ${resp.json()['elements']['709c62b3-8918-41b9-a747-d21eb79c6c35']['outProperties']['rollbackStage']}
    Should Be Equal As Strings  ${respstring}  [1, 0]
    ${respstring}   Convert To String   ${resp.json()['elements']['709c62b3-8918-41b9-a747-d21eb79c6c36']['outProperties']['rollbackStage']}
    Should Be Equal As Strings  ${respstring}  [2, 0]

VerifyMigratedElementsSim
    [Arguments]  ${theInstanceId}
    [Documentation]  Query on Participant Simulator 1
    ${auth}=    ParticipantAuth
    ${resp}=    MakeGetRequest  participant  ${HTTP_PARTICIPANT_SIM1_IP}  /onap/policy/simparticipant/v2/instances/${theInstanceId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    ${respstring}   Convert To String   ${resp.json()}
    Should Match Regexp  ${respstring}  Sim_NewAutomationCompositionElement
    Should Not Match Regexp  ${respstring}  Sim_SinkAutomationCompositionElement

VerifyMigratedElementsSim3
    [Arguments]  ${theInstanceId}
    [Documentation]  Query on Participant Simulator 3
    ${auth}=    ParticipantAuth
    ${resp}=    MakeGetRequest  participant  ${HTTP_PARTICIPANT_SIM3_IP}  /onap/policy/simparticipant/v2/instances/${theInstanceId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    ${respstring}   Convert To String   ${resp.json()}
    Should Match Regexp  ${respstring}  Sim_NewAutomationCompositionElement2
    Should Not Match Regexp  ${respstring}  Sim_SinkAutomationCompositionElement

VerifyRemovedElementsSim
    [Arguments]  ${theInstanceId}
    [Documentation]  Query on Participant Simulator 2
    ${auth}=    ParticipantAuth
    ${resp}=    MakeGetRequest  participant  ${HTTP_PARTICIPANT_SIM2_IP}  /onap/policy/simparticipant/v2/instances/${theInstanceId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    Should Be Empty    ${resp.text}

VerifyRollbackElementsSim
    [Arguments]  ${theInstanceId}
    [Documentation]  Query on Participant Simulator
    ${auth}=    ParticipantAuth
    ${resp}=    MakeGetRequest  participant  ${HTTP_PARTICIPANT_SIM1_IP}  /onap/policy/simparticipant/v2/instances/${theInstanceId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    ${respstring}   Convert To String   ${resp.json()}
    Should Not Match Regexp  ${respstring}  Sim_NewAutomationCompositionElement
    Should Match Regexp  ${respstring}  Sim_SinkAutomationCompositionElement

VerifyCompositionParticipantSim
    [Arguments]  ${textToFind}
    [Documentation]  Query composition on Participant Simulator
    ${auth}=    ParticipantAuth
    ${resp}=    MakeGetRequest  participant  ${HTTP_PARTICIPANT_SIM1_IP}  /onap/policy/simparticipant/v2/compositiondatas  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    ${respstring}   Convert To String   ${resp.json()}
    Should Match Regexp  ${respstring}  ${textToFind}

VerifyParticipantSim
    [Arguments]  ${theInstanceId}  ${textToFind}
    [Documentation]  Query on Participant Simulator
    ${auth}=    ParticipantAuth
    ${resp}=    MakeGetRequest  participant  ${HTTP_PARTICIPANT_SIM1_IP}  /onap/policy/simparticipant/v2/instances/${theInstanceId}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    ${respstring}   Convert To String   ${resp.json()}
    Should Match Regexp  ${respstring}  ${textToFind}

VerifyUninstantiated
    [Arguments]  ${theCompositionId}
    [Documentation]  Verify the Uninstantiation of automation composition.
    ${auth}=    ClampAuth
    ${resp}=    MakeGetRequest  ACM  ${POLICY_RUNTIME_ACM_IP}   /onap/policy/clamp/acm/v2/compositions/${theCompositionId}/instances  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    Run Keyword If  ${resp.status_code}==200  Length Should Be  ${resp.json()['automationCompositionList']}  0

SetParticipantSimFail
    [Arguments]  ${domain}
    [Documentation]  Set Participant Simulator Fail.
    ${auth}=    ParticipantAuth
    ${postjson}=  Get file  ${CURDIR}/data/SettingSimPropertiesFail.json
    ${resp}=   MakeJsonPutRequest  participant  ${domain}  /onap/policy/simparticipant/v2/parameters  ${postjson}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200

SetParticipantSimSuccess
    [Arguments]  ${domain}
    [Documentation]  Set Participant Simulator Success.
    ${auth}=    ParticipantAuth
    ${postjson}=  Get file  ${CURDIR}/data/SettingSimPropertiesSuccess.json
    ${resp}=   MakeJsonPutRequest  participant  ${domain}  /onap/policy/simparticipant/v2/parameters  ${postjson}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200

SetParticipantSimTimeout
    [Arguments]  ${domain}
    [Documentation]  Set Participant Simulator Timeout.
    ${auth}=    ParticipantAuth
    ${postjson}=  Get file  ${CURDIR}/data/SettingSimPropertiesTimeout.json
    ${resp}=   MakeJsonPutRequest  participant  ${domain}  /onap/policy/simparticipant/v2/parameters  ${postjson}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200

SetParticipantSimDelay
    [Documentation]  Set Participant Simulator Delay.
    ${auth}=    ParticipantAuth
    ${postjson}=  Get file  ${CURDIR}/data/SettingSimPropertiesDelay.json
    ${resp}=   MakeJsonPutRequest  participant  ${HTTP_PARTICIPANT_SIM1_IP}  /onap/policy/simparticipant/v2/parameters  ${postjson}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200


ClampAuth
    ${auth}=    Create List    runtimeUser    zb!XztG34
    RETURN  ${auth}

ParticipantAuth
    ${auth}=    Create List    participantUser    zb!XztG34
    RETURN  ${auth}

MakeCommissionAcDefinition
    [Arguments]   ${postyaml}
    ${auth}=    ClampAuth
    ${resp}=   MakeYamlPostRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions  ${postyaml}  ${auth}
    ${respyaml}=  yaml.Safe Load  ${resp.text}
    Should Be Equal As Strings    ${resp.status_code}     201
    RETURN  ${respyaml["compositionId"]}

DePrimeAndDeleteACDefinition
    [Arguments]   ${compositionId}
    ${postjson}=  Get file  ${CURDIR}/data/ACDepriming.json
    PrimeACDefinition  ${postjson}  ${compositionId}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyPriming  ${compositionId}  COMMISSIONED
    DeleteACDefinition  ${compositionId}

PrimeACDefinition
    [Arguments]   ${postjson}  ${compositionId}
    ${auth}=    ClampAuth
    ${resp}=   MakeJsonPutRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${compositionId}  ${postjson}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     202

DeleteACDefinition
    [Arguments]   ${compositionId}
    ${auth}=    ClampAuth
    Log    Creating session http://${POLICY_RUNTIME_ACM_IP}
    ${session}=    Create Session      policy  http://${POLICY_RUNTIME_ACM_IP}   auth=${auth}
    ${headers}=  Create Dictionary     Accept=application/yaml    Content-Type=application/yaml
    ${resp}=   DELETE On Session     policy  /onap/policy/clamp/acm/v2/compositions/${compositionId}  headers=${headers}
    Log    Received response from runtime acm ${resp.text}
    Should Be Equal As Strings    ${resp.status_code}     200

MakeJsonInstantiateAutomationComposition
    [Arguments]  ${compositionId}  ${postjson}
    ${auth}=    ClampAuth
    ${updatedpostjson}=   Replace String     ${postjson}     COMPOSITIONIDPLACEHOLDER       ${compositionId}
    ${resp}=   MakeJsonPostRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${compositionId}/instances  ${updatedpostjson}  ${auth}
    ${respyaml}=  yaml.Safe Load  ${resp.text}
    Should Be Equal As Strings    ${resp.status_code}     201
    RETURN  ${respyaml["instanceId"]}

MakeYamlInstantiateAutomationComposition
    [Arguments]  ${compositionId}  ${postyaml}
    ${auth}=    ClampAuth
    ${updatedpostyaml}=   Replace String     ${postyaml}     COMPOSITIONIDPLACEHOLDER       ${compositionId}
    ${resp}=   MakeYamlPostRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${compositionId}/instances  ${updatedpostyaml}  ${auth}
    ${respyaml}=  yaml.Safe Load  ${resp.text}
    Should Be Equal As Strings    ${resp.status_code}     201
    RETURN  ${respyaml["instanceId"]}

ChangeStatusAutomationComposition
    [Arguments]  ${compositionId}   ${instanceId}   ${postjson}
    ${auth}=    ClampAuth
    ${resp}=   MakeJsonPutRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${compositionId}/instances/${instanceId}  ${postjson}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     202

DeleteAutomationComposition
    [Arguments]  ${compositionId}  ${instanceId}
    ${auth}=    ClampAuth
    Log    Creating session http://${POLICY_RUNTIME_ACM_IP}
    ${session}=    Create Session      policy  http://${POLICY_RUNTIME_ACM_IP}   auth=${auth}
    ${headers}=  Create Dictionary     Accept=application/json    Content-Type=application/json
    ${resp}=   DELETE On Session     policy  /onap/policy/clamp/acm/v2/compositions/${compositionId}/instances/${instanceId}     headers=${headers}
    Log    Received response from runtime acm ${resp.text}
    Should Be Equal As Strings    ${resp.status_code}     202

UndeployAndDeleteAutomationComposition
    [Arguments]  ${compositionId}  ${instanceId}
    ${postjson}=  Get file  ${CURDIR}/data/UndeployAC.json
    ChangeStatusAutomationComposition  ${compositionId}   ${instanceId}  ${postjson}
    Wait Until Keyword Succeeds    3 min    5 sec    VerifyDeployStatus  ${compositionId}  ${instanceId}  UNDEPLOYED
    DeleteAutomationComposition  ${compositionId}  ${instanceId}
    Wait Until Keyword Succeeds    1 min    5 sec    VerifyUninstantiated  ${compositionId}

MigrateAc
    [Arguments]   ${postyaml}  ${theCompositionId}  ${theCompositionTargetId}  ${theInstanceId}  ${theText}
    ${auth}=    ClampAuth
    ${updatedpostyaml}=   Replace String     ${postyaml}            COMPOSITIONIDPLACEHOLDER             ${theCompositionId}
    ${updatedpostyaml}=   Replace String     ${updatedpostyaml}     COMPOSITIONTARGETIDPLACEHOLDER       ${theCompositionTargetId}
    ${updatedpostyaml}=   Replace String     ${updatedpostyaml}     INSTACEIDPLACEHOLDER                 ${theInstanceId}
    ${updatedpostyaml}=   Replace String     ${updatedpostyaml}     TEXTPLACEHOLDER                      ${theText}
    ${resp}=   MakeYamlPostRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${theCompositionId}/instances  ${updatedpostyaml}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200

MakePostRequest
    [Arguments]  ${name}  ${domain}  ${url}  ${auth}
    Log    Creating session http://${domain}
    ${session}=  Create Session  ${name}  http://${domain}  auth=${auth}
    ${resp}=   POST On Session     ${name}  ${url}
    Log    Received response from ${name} ${resp.text}
    RETURN  ${resp}

MakeYamlPostRequest
    [Arguments]  ${name}  ${domain}  ${url}  ${postyaml}  ${auth}
    Log  Creating session http://${domain}
    ${session}=  Create Session  ${name}  http://${domain}  auth=${auth}
    ${headers}  Create Dictionary     Accept=application/yaml    Content-Type=application/yaml
    ${resp}=  POST On Session  ${name}  ${url}  data=${postyaml}  headers=${headers}
    Log  Received response from ${name} ${resp.text}
    RETURN  ${resp}

MakeJsonPostRequest
    [Arguments]  ${name}  ${domain}  ${url}  ${postjson}  ${auth}
    Log  Creating session http://${domain}
    ${session}=  Create Session  ${name}  http://${domain}  auth=${auth}
    ${headers}=  Create Dictionary     Accept=application/json    Content-Type=application/json
    ${resp}=  POST On Session  ${name}  ${url}  data=${postjson}  headers=${headers}
    Log  Received response from ${name} ${resp.text}
    RETURN  ${resp}

MakeJsonPutRequest
    [Arguments]  ${name}  ${domain}  ${url}  ${postjson}  ${auth}
    Log  Creating session http://${domain}
    ${session}=  Create Session  ${name}  http://${domain}  auth=${auth}
    ${headers}=  Create Dictionary     Accept=application/json    Content-Type=application/json
    ${resp}=  PUT On Session  ${name}  ${url}  data=${postjson}  headers=${headers}
    Log  Received response from ${name} ${resp.text}
    RETURN  ${resp}

MakeGetRequest
    [Arguments]  ${name}  ${domain}  ${url}  ${auth}
    Log    Creating session http://${domain}
    ${session}=    Create Session      ${name}  http://${domain}   auth=${auth}
    ${headers}=  Create Dictionary     Accept=application/json    Content-Type=application/json
    ${resp}=   GET On Session     ${name}  ${url}     headers=${headers}
    Log    Received response from ${name} ${resp.text}
    RETURN  ${resp}

VerifyKafkaInTraces
    [Arguments]  ${domain}    ${service}
    Log  Creating session http://${domain}
    ${session}=  Create Session  jaeger  http://${domain}
    ${tags}=    Create Dictionary    otel.library.name=io.opentelemetry.kafka-clients-2.6    messaging.system=kafka
    ${tags_json}=    evaluate    json.dumps(${tags})    json
    ${params}=    Create Dictionary    service=${service}    tags=${tags_json}    operation=policy-acruntime-participant publish    lookback=1h    limit=10
    ${resp}=  GET On Session  jaeger  /api/traces  params=${params}    expected_status=200
    Log  Received response from jaeger ${resp.text}
    RETURN  ${resp}

VerifyHttpInTraces
    [Arguments]  ${domain}    ${service}
    Log  Creating session http://${domain}
    ${session}=  Create Session  jaeger  http://${domain}
    ${tags}=    Create Dictionary    uri=/v2/compositions/{compositionId}
    ${tags_json}=    evaluate    json.dumps(${tags})    json
    ${params}=    Create Dictionary    service=${service}    tags=${tags_json}    operation=http put /v2/compositions/{compositionId}    lookback=1h    limit=10
    ${resp}=  GET On Session  jaeger  /api/traces  params=${params}    expected_status=200
    Log  Received response from jaeger ${resp.text}
    RETURN  ${resp}

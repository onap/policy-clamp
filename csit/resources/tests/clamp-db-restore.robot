*** Settings ***
Name        Database Restore Suite
Library     Collections
Library     RequestsLibrary
Library     OperatingSystem
Library     String
Library     json
Library     yaml
Library     Process
Resource    common-library.robot
Resource    clamp-common.robot

*** Test Cases ***

InsertDataIntoDatabase
    [Documentation]  Insert restored data into the Database.
    ExecuteQuery    ${CURDIR}/data/query/compositiondefinition-from.sql
    ExecuteQuery    ${CURDIR}/data/query/compositiondefinition-to.sql
    ExecuteQuery    ${CURDIR}/data/query/instance.sql

AcMigrationRestored
    [Documentation]  Migration of an automation composition restored.
    set Suite variable  ${compositionIdRestored}  d30b8017-4d64-4693-84d7-de9c4226b9f8
    set Suite variable  ${InstanceIdRestored}  dd36aaa4-580f-4193-a52b-37c3a955b11a
    set Suite variable  ${compositionTargetIdRestored}  6c1cf107-a2ca-4485-8129-02f9fae64d64
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    ${auth}=    ClampAuth
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-migration-restored.yaml
    ${resp}=   MakeYamlPostRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${compositionIdRestored}/instances  ${postyaml}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyDeployStatus  ${compositionTargetIdRestored}  ${InstanceIdRestored}  DEPLOYED

UpdateDataDatabase
    [Documentation]  Update restored data into the Database.
    ExecuteQuery    ${CURDIR}/data/query/compositiondefinition-update.sql

ReviewAutomationCompositionRestored
    [Documentation]  Review automation composition restored.
    ${postjson}=  Get file  ${CURDIR}/data/ReviewAC.json
    ChangeStatusAutomationComposition  ${compositionTargetIdRestored}   ${InstanceIdRestored}  ${postjson}
    Wait Until Keyword Succeeds    10 min    5 sec    VerifySubStatus  ${compositionTargetIdRestored}  ${InstanceIdRestored}
    VerifyCompositionParticipantSim   'MyTextUpdated'

AcDeleteRestored
    [Documentation]  Undeploy and delete of an automation composition restored.
    UndeployAndDeleteAutomationComposition  ${compositionTargetIdRestored}  ${InstanceIdRestored}

DeleteACDefinitionsRestored
    [Documentation]  Deprime and delete of the compositions definition restored.
    DePrimeAndDeleteACDefinition   ${compositionIdRestored}
    DePrimeAndDeleteACDefinition   ${compositionTargetIdRestored}

InsertDataIntoDatabase2
    [Documentation]  Insert restored data into the Database.
    ExecuteQuery    ${CURDIR}/data/query/compositiondefinition-from.sql
    ExecuteQuery    ${CURDIR}/data/query/instance.sql

SyncParticipant
    [Documentation]  Manual sync participants.
    ${auth}=    ClampAuth
    Log    Creating session http://${POLICY_RUNTIME_ACM_IP}
    ${session}=    Create Session      policy  http://${POLICY_RUNTIME_ACM_IP}   auth=${auth}
    ${resp}=   PUT On Session     policy  /onap/policy/clamp/acm/v2/participants/sync
    Log    Received response from runtime acm ${resp.text}
    Should Be Equal As Strings    ${resp.status_code}     202
    Wait Until Keyword Succeeds    1 min    10 sec    VerifyCompositionParticipantSim   'InternalState'
    VerifyParticipantSim  ${InstanceIdRestored}  myParameterToUpdate

GetInstances
    [Documentation]    Get all the instances from the database
    ${auth}=    ClampAuth
    Log    Creating session http://${POLICY_RUNTIME_ACM_IP}
    ${session}=    Create Session    policy    http://${POLICY_RUNTIME_ACM_IP}    auth=${auth}
    ${resp}=    GET On Session    policy    /onap/policy/clamp/acm/v2/instances
    Log    Received response from ACM-R ${resp.text}
    Should Be Equal As Strings    ${resp.status_code}    200
    Length Should Be    ${resp.json()["automationCompositionList"]}    1

AcDeleteRestored2
    [Documentation]  Undeploy and delete of an automation composition restored.
    UndeployAndDeleteAutomationComposition  ${compositionIdRestored}  ${InstanceIdRestored}
    DePrimeAndDeleteACDefinition   ${compositionIdRestored}

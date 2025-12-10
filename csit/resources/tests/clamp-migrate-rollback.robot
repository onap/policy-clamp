*** Settings ***
Name        Migration and Rollback Suite
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
CommissionAutomationComposition
    [Documentation]  Commission automation composition definition.
    Run Keyword If    '${TEST_ENV}'=='k8s'    set Suite variable  ${compositionFile}  acelement-usecase.yaml

    ...    ELSE    set Suite variable  ${compositionFile}  acelement-usecaseDocker.yaml
    ${postyaml}=  Get file  ${CURDIR}/data/${compositionFile}
    ${tmpCompositionId}=  MakeCommissionAcDefinition  ${postyaml}
    set Suite variable  ${compositionId}  ${tmpCompositionId}

CommissionAcDefinitionMigrationFrom
    [Documentation]  Commission automation composition definition From.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-definition-migration-from.yaml
    ${tmpCompositionId}=  MakeCommissionAcDefinition  ${postyaml}
    set Suite variable  ${compositionFromId}  ${tmpCompositionId}

CommissionAcDefinitionMigrationTo
    [Documentation]  Commission automation composition definition To.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-definition-migration-to.yaml
    ${tmpCompositionId}=  MakeCommissionAcDefinition  ${postyaml}
    set Suite variable  ${compositionToId}  ${tmpCompositionId}

PrimeACDefinitions
    [Documentation]  Prime automation composition definition
    ${postjson}=  Get file  ${CURDIR}/data/ACPriming.json
    PrimeACDefinition  ${postjson}  ${compositionId}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyPriming  ${compositionId}  PRIMED

FailPrimeACDefinitionFrom
    [Documentation]  Prime automation composition definition Migration From with Fail.
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postjson}=  Get file  ${CURDIR}/data/ACPriming.json
    PrimeACDefinition  ${postjson}  ${compositionFromId}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResultPriming  ${compositionFromId}   FAILED

PrimeACDefinitionFrom
    [Documentation]  Prime automation composition definition Migration From.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postjson}=  Get file  ${CURDIR}/data/ACPriming.json
    PrimeACDefinition  ${postjson}  ${compositionFromId}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyPriming  ${compositionFromId}  PRIMED

PrimeACDefinitionTo
    [Documentation]  Prime automation composition definition Migration To.
    ${postjson}=  Get file  ${CURDIR}/data/ACPriming.json
    PrimeACDefinition  ${postjson}  ${compositionToId}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyPriming  ${compositionToId}  PRIMED

DeleteUndeployedInstantiateAutomationComposition
    [Documentation]  Delete Instantiate automation composition that has never been deployed.
    Run Keyword If    '${TEST_ENV}'=='k8s'    set Suite variable  ${instantiationfile}  AcK8s.json

    ...    ELSE    set Suite variable  ${instantiationfile}  AcDocker.json
    ${postjson}=  Get file  ${CURDIR}/data/${instantiationfile}
    ${tmpInstanceId}=  MakeJsonInstantiateAutomationComposition  ${compositionId}  ${postjson}
    DeleteAutomationComposition  ${compositionId}  ${tmpInstanceId}
    Wait Until Keyword Succeeds    3 min    5 sec    VerifyUninstantiated  ${compositionId}

InstantiateAutomationComposition
    [Documentation]  Instantiate automation composition.
    Run Keyword If    '${TEST_ENV}'=='k8s'    set Suite variable  ${instantiationfile}  AcK8s.json

    ...    ELSE    set Suite variable  ${instantiationfile}  AcDocker.json
    ${postjson}=  Get file  ${CURDIR}/data/${instantiationfile}
    ${tmpInstanceId}=  MakeJsonInstantiateAutomationComposition  ${compositionId}  ${postjson}
    Set Suite Variable  ${instanceId}  ${tmpInstanceId}

InstantiateAutomationCompositionTimeout
    [Documentation]  Instantiate automation composition timeout.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-timeout.yaml
    ${tmpInstanceId}=  MakeYamlInstantiateAutomationComposition   ${compositionFromId}   ${postyaml}
    set Suite variable  ${instanceTimeoutId}    ${tmpInstanceId}

DeployAutomationCompositionTimeout
    [Documentation]  Deploy automation composition timeout.
    SetParticipantSimTimeout  ${HTTP_PARTICIPANT_SIM1_IP}
    SetParticipantSimTimeout  ${HTTP_PARTICIPANT_SIM2_IP}
    ${postjson}=  Get file  ${CURDIR}/data/DeployAC.json
    ChangeStatusAutomationComposition  ${compositionFromId}  ${instanceTimeoutId}   ${postjson}
    Wait Until Keyword Succeeds    5 min    5 sec    VerifyStateChangeResult  ${compositionFromId}  ${instanceTimeoutId}  TIMEOUT

DeleteAutomationCompositionTimeout
    [Documentation]  Delete automation composition timeout.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM2_IP}
    UndeployAndDeleteAutomationComposition  ${compositionFromId}  ${instanceTimeoutId}

InstantiateAutomationCompositionMigrationFrom
    [Documentation]  Instantiate automation composition migration.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-migration-from.yaml
    ${updatedpostyaml}=   Replace String     ${postyaml}     TEXTPLACEHOLDER       MyTextInit
    ${tmpInstanceId}=  MakeYamlInstantiateAutomationComposition   ${compositionFromId}   ${updatedpostyaml}
    set Suite variable  ${instanceMigrationId}    ${tmpInstanceId}

FailPrepareAutomationCompositionMigrationFrom
    [Documentation]  Fail Prepare automation composition migration.
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM1_IP}
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM2_IP}
    ${postjson}=  Get file  ${CURDIR}/data/PrepareAC.json
    ChangeStatusAutomationComposition   ${compositionFromId}   ${instanceMigrationId}  ${postjson}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResult  ${compositionFromId}  ${instanceMigrationId}  FAILED

PrepareAutomationCompositionMigrationFrom
    [Documentation]  Prepare automation composition migration.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM2_IP}
    ${postjson}=  Get file  ${CURDIR}/data/PrepareAC.json
    ChangeStatusAutomationComposition   ${compositionFromId}   ${instanceMigrationId}  ${postjson}
    Wait Until Keyword Succeeds    10 min    5 sec    VerifySubStatus  ${compositionFromId}  ${instanceMigrationId}
    VerifyPrepareElementsRuntime   ${compositionFromId}  ${instanceMigrationId}

FailDeployAutomationCompositionMigration
    [Documentation]  Fail Deploy automation composition.
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postjson}=  Get file  ${CURDIR}/data/DeployAC.json
    ChangeStatusAutomationComposition  ${compositionFromId}   ${instanceMigrationId}  ${postjson}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResult  ${compositionFromId}  ${instanceMigrationId}  FAILED

DeployAutomationComposition
    [Documentation]  Deploy automation composition.
    ${postjson}=  Get file  ${CURDIR}/data/DeployAC.json
    ChangeStatusAutomationComposition  ${compositionId}  ${instanceId}  ${postjson}
    Wait Until Keyword Succeeds    10 min    5 sec    VerifyDeployStatus  ${compositionId}  ${instanceId}  DEPLOYED

QueryPolicies
    [Documentation]    Verify the new policies deployed
    ${auth}=    PolicyAdminAuth
    Sleep  10s
    Log    Creating session http://${POLICY_PAP_IP}
    ${session}=    Create Session      policy  http://${POLICY_PAP_IP}   auth=${auth}
    ${headers}=  Create Dictionary     Accept=application/json    Content-Type=application/json
    ${resp}=   GET On Session     policy  /policy/pap/v1/policies/deployed     headers=${headers}
    Log    Received response from policy-pap {resp.text}
    Should Be Equal As Strings    ${resp.status_code}     200
    Dictionary Should Contain Value  ${resp.json()[0]}  onap.policies.native.apex.ac.element

QueryPolicyTypes
    [Documentation]    Verify the new policy types created
    ${auth}=    PolicyAdminAuth
    Sleep  10s
    Log    Creating session http://${POLICY_API_IP}:6969
    ${session}=    Create Session      policy  http://${POLICY_API_IP}   auth=${auth}
    ${headers}=  Create Dictionary     Accept=application/json    Content-Type=application/json
    ${resp}=   GET On Session     policy  /policy/api/v1/policytypes     headers=${headers}
    Log    Received response from policy-api ${resp.text}
    Should Be Equal As Strings    ${resp.status_code}     200
    List Should Contain Value  ${resp.json()['policy_types']}  onap.policies.native.Apex

DeployAutomationCompositionMigration
    [Documentation]  Deploy automation composition.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postjson}=  Get file  ${CURDIR}/data/DeployAC.json
    ChangeStatusAutomationComposition  ${compositionFromId}   ${instanceMigrationId}  ${postjson}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyDeployStatus  ${compositionFromId}  ${instanceMigrationId}  DEPLOYED

SendOutPropertiesToRuntime
    [Documentation]  Send Out Properties To Runtime
    ${auth}=    ParticipantAuth
    ${postjson}=  Get file  ${CURDIR}/data/OutProperties.json
    ${updatedpostjson}=   Replace String     ${postjson}     INSTACEIDPLACEHOLDER       ${instanceMigrationId}
    ${updatedpostjson}=   Replace String     ${updatedpostjson}     TEXTPLACEHOLDER       DumpTest
    ${resp}=   MakeJsonPutRequest  participant  ${HTTP_PARTICIPANT_SIM1_IP}  /onap/policy/simparticipant/v2/datas  ${updatedpostjson}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    ${resp}=   MakeJsonPutRequest  participant  ${HTTP_PARTICIPANT_SIM1_IP}  /onap/policy/simparticipant/v2/datas  ${updatedpostjson}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    ${updatedpostjson}=   Replace String     ${postjson}     INSTACEIDPLACEHOLDER       ${instanceMigrationId}
    ${updatedpostjson}=   Replace String     ${updatedpostjson}     TEXTPLACEHOLDER       MyTextToSend
    ${resp}=   MakeJsonPutRequest  participant  ${HTTP_PARTICIPANT_SIM1_IP}  /onap/policy/simparticipant/v2/datas  ${updatedpostjson}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyPropertiesUpdated  ${compositionFromId}  ${instanceMigrationId}  MyTextToSend

FailReviewAutomationCompositionMigrationFrom
    [Documentation]  Fail Review automation composition migration.
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postjson}=  Get file  ${CURDIR}/data/ReviewAC.json
    ChangeStatusAutomationComposition  ${compositionFromId}   ${instanceMigrationId}  ${postjson}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResult  ${compositionFromId}  ${instanceMigrationId}  FAILED

ReviewAutomationCompositionMigrationFrom
    [Documentation]  Review automation composition migration.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postjson}=  Get file  ${CURDIR}/data/ReviewAC.json
    ChangeStatusAutomationComposition  ${compositionFromId}   ${instanceMigrationId}  ${postjson}
    Wait Until Keyword Succeeds    10 min    5 sec    VerifySubStatus  ${compositionFromId}  ${instanceMigrationId}

AutomationCompositionUpdate
    [Documentation]  Update of an automation composition.
    ${auth}=    ClampAuth
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-update.yaml
    ${updatedpostyaml}=   Replace String     ${postyaml}     COMPOSITIONIDPLACEHOLDER       ${compositionFromId}
    ${updatedpostyaml}=   Replace String     ${updatedpostyaml}     INSTACEIDPLACEHOLDER       ${instanceMigrationId}
    ${updatedpostyaml}=   Replace String     ${updatedpostyaml}     TEXTPLACEHOLDER       MyTextUpdated
    ${resp}=   MakeYamlPostRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${compositionFromId}/instances  ${updatedpostyaml}  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyDeployStatus  ${compositionFromId}  ${instanceMigrationId}  DEPLOYED
    VerifyPropertiesUpdated  ${compositionFromId}  ${instanceMigrationId}  MyTextUpdated
    VerifyParticipantSim  ${instanceMigrationId}  MyTextUpdated

PrecheckAutomationCompositionMigration
    [Documentation]  Precheck Migration of an automation composition.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-precheck-migration.yaml
    MigrateAc  ${postyaml}  ${compositionFromId}  ${compositionToId}  ${instanceMigrationId}  TextForMigration
    Wait Until Keyword Succeeds    2 min    5 sec    VerifySubStatus  ${compositionFromId}  ${instanceMigrationId}

AutomationCompositionMigrationTo
    [Documentation]  Migration of an automation composition.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-migration-to.yaml
    MigrateAc  ${postyaml}  ${compositionFromId}  ${compositionToId}  ${instanceMigrationId}  TextForMigration
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyDeployStatus  ${compositionToId}  ${instanceMigrationId}  DEPLOYED
    VerifyPropertiesUpdated  ${compositionToId}  ${instanceMigrationId}  TextForMigration
    VerifyParticipantSim  ${instanceMigrationId}  TextForMigration
    VerifyMigratedElementsRuntime  ${compositionToId}  ${instanceMigrationId}
    VerifyMigratedElementsSim  ${instanceMigrationId}
    VerifyRemovedElementsSim  ${instanceMigrationId}
    VerifyMigratedElementsSim3  ${instanceMigrationId}

FailAutomationCompositionMigration
    [Documentation]  Fail Migration of an automation composition.
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-migration-fail.yaml
    MigrateAc  ${postyaml}  ${compositionToId}  ${compositionFromId}  ${instanceMigrationId}  TextForMigration
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResult  ${compositionToId}  ${instanceMigrationId}  FAILED

UnInstantiateAutomationComposition
    [Documentation]  UnDeploy and Delete automation composition instance.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    UndeployAndDeleteAutomationComposition  ${compositionId}  ${instanceId}

FailUnDeployAutomationCompositionMigrationTo
    [Documentation]  Fail UnDeploy automation composition migrated.
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postjson}=  Get file  ${CURDIR}/data/UndeployAC.json
    ChangeStatusAutomationComposition  ${compositionToId}   ${instanceMigrationId}  ${postjson}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResult  ${compositionToId}  ${instanceMigrationId}  FAILED

UnDeployAutomationCompositionMigrationTo
    [Documentation]  UnDeploy automation composition migrated.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postjson}=  Get file  ${CURDIR}/data/UndeployAC.json
    ChangeStatusAutomationComposition  ${compositionToId}   ${instanceMigrationId}  ${postjson}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyDeployStatus  ${compositionToId}  ${instanceMigrationId}  UNDEPLOYED

FailUnInstantiateAutomationCompositionMigrationTo
    [Documentation]  Fail Delete automation composition instance migrated.
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM1_IP}
    DeleteAutomationComposition  ${compositionToId}  ${instanceMigrationId}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResult  ${compositionToId}  ${instanceMigrationId}  FAILED

UnInstantiateAutomationCompositionMigrationTo
    [Documentation]  Delete automation composition instance migrated.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    DeleteAutomationComposition  ${compositionToId}  ${instanceMigrationId}
    Wait Until Keyword Succeeds    1 min    5 sec    VerifyUninstantiated  ${compositionToId}

InstantiateAutomationCompositionRollback
    [Documentation]  Instantiate automation composition for testing rollback.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-migration-from.yaml
    ${updatedpostyaml}=   Replace String     ${postyaml}     TEXTPLACEHOLDER       MyTextInit
    ${tmpInstanceId}=  MakeYamlInstantiateAutomationComposition   ${compositionFromId}   ${updatedpostyaml}
    set Suite variable  ${instanceMigrationId}    ${tmpInstanceId}

DeployAutomationCompositionRollback
    [Documentation]  Deploy automation for testing rollback.
    ${auth}=    ClampAuth
    ${postjson}=  Get file  ${CURDIR}/data/DeployAC.json
    ChangeStatusAutomationComposition  ${compositionFromId}   ${instanceMigrationId}  ${postjson}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyDeployStatus  ${compositionFromId}  ${instanceMigrationId}  DEPLOYED

FailAutomationCompositionMigrationRollback
    [Documentation]  Fail Migration of an automation composition for testing rollback.
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-migration-to.yaml
    MigrateAc  ${postyaml}  ${compositionFromId}  ${compositionToId}  ${instanceMigrationId}  TextForMigration
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResult  ${compositionFromId}  ${instanceMigrationId}  FAILED

RollbackAutomationComposition
    [Documentation]  Rollback of an automation composition.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    ${auth}=    ClampAuth
    ${resp}=   MakePostRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${compositionFromId}/instances/${instanceMigrationId}/rollback  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     202
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyDeployStatus  ${compositionFromId}  ${instanceMigrationId}  DEPLOYED
    VerifyPropertiesUpdated  ${compositionFromId}  ${instanceMigrationId}  MyTextInit
    VerifyParticipantSim  ${instanceMigrationId}  MyTextInit
    VerifyRollbackElementsRuntime  ${compositionFromId}  ${instanceMigrationId}
    VerifyRollbackElementsSim  ${instanceMigrationId}

UnInstantiateAutomationCompositionRollback
    [Documentation]  Undeploy and Delete automation composition instance in fail rollback.
    UndeployAndDeleteAutomationComposition  ${compositionFromId}  ${instanceMigrationId}

InstantiateAutomationCompositionRollback2
    [Documentation]  Instantiate automation composition for testing rollback.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-migration-from.yaml
    ${updatedpostyaml}=   Replace String     ${postyaml}     TEXTPLACEHOLDER       MyTextInit
    ${tmpInstanceId}=  MakeYamlInstantiateAutomationComposition   ${compositionFromId}   ${updatedpostyaml}
    set Suite variable  ${instanceMigrationId}    ${tmpInstanceId}

DeployAutomationCompositionRollback2
    [Documentation]  Deploy automation for testing rollback.
    ${auth}=    ClampAuth
    ${postjson}=  Get file  ${CURDIR}/data/DeployAC.json
    ChangeStatusAutomationComposition  ${compositionFromId}   ${instanceMigrationId}  ${postjson}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyDeployStatus  ${compositionFromId}  ${instanceMigrationId}  DEPLOYED

FailAutomationCompositionMigrationRollback2
    [Documentation]  Fail Migration of an automation composition for testing rollback.
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM2_IP}
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-migration-to.yaml
    MigrateAc  ${postyaml}  ${compositionFromId}  ${compositionToId}  ${instanceMigrationId}  TextForMigration
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResult  ${compositionFromId}  ${instanceMigrationId}  FAILED

RollbackAutomationComposition2
    [Documentation]  Rollback of an automation composition.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM2_IP}
    ${auth}=    ClampAuth
    ${resp}=   MakePostRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${compositionFromId}/instances/${instanceMigrationId}/rollback  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     202
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyDeployStatus  ${compositionFromId}  ${instanceMigrationId}  DEPLOYED
    VerifyPropertiesUpdated  ${compositionFromId}  ${instanceMigrationId}  MyTextInit
    VerifyParticipantSim  ${instanceMigrationId}  MyTextInit
    VerifyRollbackElementsRuntime  ${compositionFromId}  ${instanceMigrationId}
    VerifyRollbackElementsSim  ${instanceMigrationId}

FailAutomationCompositionMigrationRollback3
    [Documentation]  Fail Migration of an automation composition for testing rollback.
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-migration-to.yaml
    MigrateAc  ${postyaml}  ${compositionFromId}  ${compositionToId}  ${instanceMigrationId}  TextForMigration
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResult  ${compositionFromId}  ${instanceMigrationId}  FAILED

FailRollbackAutomationComposition
    [Documentation]  Fail Rollback of an automation composition.
    ${auth}=    ClampAuth
    ${resp}=   MakePostRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  /onap/policy/clamp/acm/v2/compositions/${compositionFromId}/instances/${instanceMigrationId}/rollback   ${auth}
    Should Be Equal As Strings    ${resp.status_code}     202
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResult  ${compositionFromId}  ${instanceMigrationId}  FAILED

UnInstantiateAutomationCompositionRollback2
    [Documentation]  Undeploy and Delete automation composition instance in fail rollback.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    UndeployAndDeleteAutomationComposition  ${compositionFromId}  ${instanceMigrationId}

FailDePrimeACDefinitionsFrom
    [Documentation]  Fail DePrime automation composition definition migration From.
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postjson}=  Get file  ${CURDIR}/data/ACDepriming.json
    PrimeACDefinition  ${postjson}  ${compositionFromId}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResultPriming  ${compositionFromId}   FAILED

DeleteACDefinitionFrom
    [Documentation]  DePrime and Delete automation composition definition migration From.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    DePrimeAndDeleteACDefinition  ${compositionFromId}

DeleteACDefinitions
    [Documentation]  DePrime and Delete automation composition definition.
    DePrimeAndDeleteACDefinition  ${compositionId}

DeleteACDefinitionTo
    [Documentation]  DePrime and Delete automation composition definition migration To.
    DePrimeAndDeleteACDefinition  ${compositionToId}


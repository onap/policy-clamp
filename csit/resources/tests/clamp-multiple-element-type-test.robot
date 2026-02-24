*** Settings ***
Name        AC with participant supporting multiple element types Workflow
Library     Collections
Library     RequestsLibrary
Library     OperatingSystem
Library     String
Library     json
Library     yaml
Library    Process
Resource    common-library.robot
Resource    clamp-common.robot

*** Test Cases ***
CommissionACMultipleElementTypes
    [Documentation]  Commission automation composition definition with multiple element types.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-definition-multiple.yaml
    ${tmpCompositionId}=  MakeCommissionAcDefinition  ${postyaml}
    set Suite variable  ${compositionId}  ${tmpCompositionId}

PrimeACDefinition
    [Documentation]  Prime automation composition definition
    ${postjson}=  Get file  ${CURDIR}/data/ACPriming.json
    PrimeACDefinition  ${postjson}  ${compositionId}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyPriming  ${compositionId}  PRIMED

InstantiateAutomationComposition
    [Documentation]  Instantiate automation composition.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-multiple.yaml
    ${tmpInstanceId}=  MakeYamlInstantiateAutomationComposition   ${compositionId}   ${postyaml}
    set Suite variable  ${instanceId}    ${tmpInstanceId}

FailDeployAutomationComposition
    [Documentation]  Fail Deploy automation composition.
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM3_IP}
    ${postjson}=  Get file  ${CURDIR}/data/DeployAC.json
    ChangeStatusAutomationComposition  ${compositionId}   ${instanceId}  ${postjson}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResult  ${compositionId}  ${instanceId}  FAILED

UnDeployAutomationComposition
    [Documentation]  UnDeploy automation composition.
    SetParticipantSimDelay  ${HTTP_PARTICIPANT_SIM3_IP}
    ${postjson}=  Get file  ${CURDIR}/data/UndeployAC.json
    ChangeStatusAutomationComposition  ${compositionId}   ${instanceId}  ${postjson}
    Wait Until Keyword Succeeds    1 min    5 sec    VerifyDeployStatus  ${compositionId}  ${instanceId}  UNDEPLOYING
    Wait Until Keyword Succeeds    1 min    5 sec    VerifyInternalStateElementsRuntime  ${compositionId}   ${instanceId}  UNDEPLOYING  709c62b3-8918-41b9-a747-d21eb80c6c41
    Wait Until Keyword Succeeds    1 min    5 sec    VerifyInternalStateElementsRuntime  ${compositionId}   ${instanceId}  UNDEPLOYING  709c62b3-8918-41b9-a747-d21eb80c6c42
    Wait Until Keyword Succeeds    3 min    5 sec    VerifyDeployStatus  ${compositionId}  ${instanceId}  UNDEPLOYED
    VerifyInternalStateElementsRuntime  ${compositionId}   ${instanceId}  UNDEPLOYED  709c62b3-8918-41b9-a747-d21eb80c6c41
    VerifyInternalStateElementsRuntime  ${compositionId}   ${instanceId}  UNDEPLOYED  709c62b3-8918-41b9-a747-d21eb80c6c42

UnInstantiateAutomationComposition
    [Documentation]  Delete automation composition instance.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM3_IP}
    DeleteAutomationComposition  ${compositionId}  ${instanceId}
    Wait Until Keyword Succeeds    1 min    5 sec    VerifyUninstantiated  ${compositionId}

DeleteACDefinition
    [Documentation]  DePrime and Delete automation composition definition.
    DePrimeAndDeleteACDefinition  ${compositionId}

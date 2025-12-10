*** Settings ***
Name        AC With Single Element Workflow
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
CommissionAutomationCompositionSimple
    [Documentation]  Commission simple automation composition definition.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-definition-simple.yaml
    ${tmpCompositionId}=  MakeCommissionAcDefinition  ${postyaml}
    set Suite variable  ${simpleCompositionId}  ${tmpCompositionId}

PrimeACDefinitionsSimple
    [Documentation]  Prime simple automation composition definition
    ${postjson}=  Get file  ${CURDIR}/data/ACPriming.json
    PrimeACDefinition  ${postjson}  ${simpleCompositionId}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyPriming  ${simpleCompositionId}  PRIMED

InstantiateAutomationCompositionSimple
    [Documentation]  Instantiate simple automation composition.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-simple.yaml
    ${tmpInstanceId}=  MakeYamlInstantiateAutomationComposition   ${simpleCompositionId}   ${postyaml}
    set Suite variable  ${simpleInstanceId}    ${tmpInstanceId}

FailDeployAutomationCompositionSimple
    [Documentation]  Fail Simple Deploy automation composition.
    SetParticipantSimFail  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postjson}=  Get file  ${CURDIR}/data/DeployAC.json
    ChangeStatusAutomationComposition  ${simpleCompositionId}   ${simpleInstanceId}  ${postjson}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResult  ${simpleCompositionId}  ${simpleInstanceId}  FAILED

UnDeployAutomationCompositionSimple
    [Documentation]  UnDeploy simple automation composition.
    SetParticipantSimDelay
    ${postjson}=  Get file  ${CURDIR}/data/UndeployAC.json
    ChangeStatusAutomationComposition  ${simpleCompositionId}   ${simpleInstanceId}  ${postjson}
    Wait Until Keyword Succeeds    1 min    5 sec    VerifyDeployStatus  ${simpleCompositionId}  ${simpleInstanceId}  UNDEPLOYING
    Wait Until Keyword Succeeds    1 min    5 sec    VerifyInternalStateElementsRuntime  ${simpleCompositionId}   ${simpleInstanceId}  UNDEPLOYING
    Wait Until Keyword Succeeds    3 min    5 sec    VerifyDeployStatus  ${simpleCompositionId}  ${simpleInstanceId}  UNDEPLOYED
    VerifyInternalStateElementsRuntime  ${simpleCompositionId}   ${simpleInstanceId}  UNDEPLOYED

UnInstantiateAutomationCompositionSimple
    [Documentation]  Delete simple automation composition instance.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    DeleteAutomationComposition  ${simpleCompositionId}  ${simpleInstanceId}
    Wait Until Keyword Succeeds    1 min    5 sec    VerifyUninstantiated  ${simpleCompositionId}

DeleteACDefinitionSimple
    [Documentation]  DePrime and Delete simple automation composition definition.
    DePrimeAndDeleteACDefinition  ${simpleCompositionId}

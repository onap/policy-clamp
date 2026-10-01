*** Settings ***
Name        Timeout Suite
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
CommissionAcDefinitionTimeout
    [Documentation]  Commission automation composition definition Timeout.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-definition-timeout.yaml
    ${tmpCompositionId}=  MakeCommissionAcDefinition  ${postyaml}
    set Suite variable  ${compositionTimeoutId}  ${tmpCompositionId}

TimeoutPrimeACDefinition
    [Documentation]  Prime automation composition definition Timeout.
    SetParticipantSimTimeout  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postjson}=  Get file  ${CURDIR}/data/ACPriming.json
    PrimeACDefinition  ${postjson}  ${compositionTimeoutId}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyStateChangeResultPriming  ${compositionTimeoutId}   TIMEOUT

DeleteACDefinitionTimeout
    [Documentation]  DePrime and Delete automation composition definition Timeout.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    DePrimeAndDeleteACDefinition  ${compositionTimeoutId}

CommissionAcDefinitionForInstanceTimeout
    [Documentation]  Commission automation composition for Instance Timeout.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-definition-migration-from.yaml
    ${tmpCompositionId}=  MakeCommissionAcDefinition  ${postyaml}
    set Suite variable  ${compositionFromId}  ${tmpCompositionId}

PrimeACDefinitionForInstanceTimeout
    [Documentation]  Prime automation composition definition for Instance Timeout.
    SetParticipantSimOutProperties  ${HTTP_PARTICIPANT_SIM1_IP}
    ${postjson}=  Get file  ${CURDIR}/data/ACPriming.json
    PrimeACDefinition  ${postjson}  ${compositionFromId}
    Wait Until Keyword Succeeds    2 min    5 sec    VerifyPriming  ${compositionFromId}  PRIMED
    VerifyCompositionParticipantSim  ${HTTP_PARTICIPANT_SIM1_IP}  ${compositionFromId}  {'CUSTOM_DATA': 'PRIMED'}

InstantiateAutomationCompositionTimeout
    [Documentation]  Instantiate automation composition timeout.
    ${postyaml}=  Get file  ${CURDIR}/data/ac-instance-timeout.yaml
    ${tmpInstanceId}=  MakeYamlInstantiateAutomationComposition   ${compositionFromId}   ${postyaml}
    set Suite variable  ${instanceTimeoutId}    ${tmpInstanceId}

PrepareAutomationCompositionTimeout
    [Documentation]  Prepare automation composition timeout.
    SetParticipantSimTimeout  ${HTTP_PARTICIPANT_SIM1_IP}
    SetParticipantSimTimeout  ${HTTP_PARTICIPANT_SIM2_IP}
    ${postjson}=  Get file  ${CURDIR}/data/PrepareAC.json
    ChangeStatusAutomationComposition  ${compositionFromId}  ${instanceTimeoutId}   ${postjson}
    Wait Until Keyword Succeeds    5 min    5 sec    VerifyStateChangeResult  ${compositionFromId}  ${instanceTimeoutId}  TIMEOUT

DeleteAutomationCompositionTimeout
    [Documentation]  Delete automation composition timeout.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM2_IP}
    DeleteAutomationComposition  ${compositionFromId}  ${instanceTimeoutId}
    Wait Until Keyword Succeeds    1 min    5 sec    VerifyUninstantiated  ${compositionFromId}

InstantiateAutomationCompositionTimeout2
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

DeleteAutomationCompositionTimeout2
    [Documentation]  Delete automation composition timeout.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM2_IP}
    UndeployAndDeleteAutomationComposition  ${compositionFromId}  ${instanceTimeoutId}

DeleteACDefinitionForInstanceTimeout
    [Documentation]  DePrime and Delete automation composition definition for Instance Timeout.
    SetParticipantSimSuccess  ${HTTP_PARTICIPANT_SIM1_IP}
    DePrimeAndDeleteACDefinition  ${compositionFromId}

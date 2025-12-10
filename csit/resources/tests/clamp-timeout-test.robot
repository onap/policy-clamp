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

*** Settings ***
Name        Health Check Suite
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
HealthcheckAcm
    [Documentation]    Healthcheck on Clamp Acm
    ${auth}=    ClampAuth
    ${resp}=    MakeGetRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  onap/policy/clamp/acm/health  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200

HealthcheckParticipantSim
    [Documentation]    Healthcheck on Participant Simulator
    ${auth}=    ParticipantAuth
    ${resp}=    MakeGetRequest  participant  ${HTTP_PARTICIPANT_SIM1_IP}  /onap/policy/simparticipant/health  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200

HealthcheckApi
    [Documentation]    Healthcheck on policy-api
    Wait Until Keyword Succeeds    5 min    10 sec    VerifyHealthcheckApi

HealthcheckPap
    [Documentation]    Healthcheck on policy-pap
    Wait Until Keyword Succeeds    5 min    10 sec    VerifyHealthcheckPap

RegisterParticipants
    [Documentation]  Register Participants.
    ${auth}=    ClampAuth
    Log    Creating session http://${POLICY_RUNTIME_ACM_IP}
    ${session}=    Create Session      policy  http://${POLICY_RUNTIME_ACM_IP}   auth=${auth}
    ${resp}=   PUT On Session     policy  /onap/policy/clamp/acm/v2/participants
    Log    Received response from runtime acm ${resp.text}
    Should Be Equal As Strings    ${resp.status_code}     202
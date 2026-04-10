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
    ${resp}=    MakeGetRequest  ACM  ${POLICY_RUNTIME_ACM_IP}  onap/policy/clamp/acm/actuator/health  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200

HealthcheckParticipantHttp
    [Documentation]    Healthcheck on Participant Simulator
    ${auth}=    ParticipantAuth
    ${resp}=    MakeGetRequest  participant  ${POLICY_HTTP_PARTICIPANT}  /onap/policy/clamp/acm/httpparticipant/health  ${auth}
    Should Be Equal As Strings    ${resp.status_code}     200

RegisterParticipants
    [Documentation]  Register Participants.
    ${auth}=    ClampAuth
    Log    Creating session http://${POLICY_RUNTIME_ACM_IP}
    ${session}=    Create Session      policy  http://${POLICY_RUNTIME_ACM_IP}   auth=${auth}
    ${resp}=   PUT On Session     policy  /onap/policy/clamp/acm/v2/participants
    Log    Received response from runtime acm ${resp.text}
    Should Be Equal As Strings    ${resp.status_code}     202
    Wait Until Keyword Succeeds    10 sec    2 sec    VerifyParticipantsRegistered

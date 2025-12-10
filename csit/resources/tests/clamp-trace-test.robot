*** Settings ***
Name        Tracing and Logs Suite
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
CheckTraces
    [Documentation]    Verify that traces are being recorded in jaeger
    Log    Verifying Jaeger traces
    ${acmResp}=    VerifyTracingWorks    ${JAEGER_IP}    acm-r
    ${httpSim1Resp}=    VerifyTracingWorks    ${JAEGER_IP}    sim-ppnt-1
    ${policyResp}=    VerifyTracingWorks    ${JAEGER_IP}    policy-ppnt
    ${httpSim2Resp}=    VerifyTracingWorks    ${JAEGER_IP}    sim-ppnt-2
    Should Not Be Empty    ${acmResp.json()["data"][0]["spans"][0]["spanID"]}
    Log  Received spanID is ${acmResp.json()["data"][0]["spans"][0]["spanID"]}
    Should Not Be Empty    ${httpSim1Resp.json()["data"][0]["spans"][0]["spanID"]}
    Should Not Be Empty    ${policyResp.json()["data"][0]["spans"][0]["spanID"]}
    Should Not Be Empty    ${httpSim2Resp.json()["data"][0]["spans"][0]["spanID"]}

CheckKafkaPresentInTraces
    [Documentation]    Verify that kafka traces are being recorded in jaeger
    Log    Verifying Kafka Jaeger traces
    ${acmResp}=    VerifyKafkaInTraces    ${JAEGER_IP}    acm-r
    ${httpSim1Resp}=    VerifyKafkaInTraces    ${JAEGER_IP}    sim-ppnt-1
    ${policyResp}=    VerifyKafkaInTraces    ${JAEGER_IP}    policy-ppnt
    ${httpSim2Resp}=    VerifyKafkaInTraces    ${JAEGER_IP}    sim-ppnt-2
    Should Not Be Empty    ${acmResp.json()["data"][0]["spans"][0]["spanID"]}
    Log  Received spanID is ${acmResp.json()["data"][0]["spans"][0]["spanID"]}
    Should Not Be Empty    ${httpSim1Resp.json()["data"][0]["spans"][0]["spanID"]}
    Should Not Be Empty    ${policyResp.json()["data"][0]["spans"][0]["spanID"]}
    Should Not Be Empty    ${httpSim2Resp.json()["data"][0]["spans"][0]["spanID"]}

CheckHttpPresentInAcmTraces
    [Documentation]    Verify that http traces are being recorded in jaeger
    Log    Verifying Http Jaeger traces
    ${acmResp}=    VerifyHttpInTraces    ${JAEGER_IP}    acm-r
    Should Not Be Empty    ${acmResp.json()["data"][0]["spans"][0]["spanID"]}
    Log  Received spanID is ${acmResp.json()["data"][0]["spans"][0]["spanID"]}
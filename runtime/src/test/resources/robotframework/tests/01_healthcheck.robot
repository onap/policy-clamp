*** Settings ***
Library     Collections
Library     RequestsLibrary
Library     OperatingSystem
Library     json
Library     OperatingSystem
*** Variables ***
${login}                     admin
${passw}                     password
*** Keywords ***
Create the sessions
*** Test Cases ***
Get Requests health check ok
    ${port} =    Get Environment Variable   CLAMP_PORT
    ${auth}=    Create List     ${login}    ${passw}
    Create Session   clamp  http://localhost:${port}   auth=${auth}   disable_warnings=1
    Set Global Variable     ${clamp_session}      clamp
    ${resp}=    Get Request    ${clamp_session}   /restservices/clds/v1/healthcheck
    Should Be Equal As Strings  ${resp.status_code}     200
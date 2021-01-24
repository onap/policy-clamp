*** Settings ***
Library     Collections
Library     RequestsLibrary
Library     OperatingSystem
Library     json
Library     OperatingSystem
*** Variables ***
${login}                     admin
${passw}                     password
${reponse_pdp_group}         pdpGroupInfo
*** Keywords ***
Create the sessions
*** Test Cases ***
Get Requests policies list ok
    ${port} =    Get Environment Variable   CLAMP_PORT
    ${auth} =    Create List     ${login}    ${passw}
    Create Session   clamp  http://localhost:${port}   auth=${auth}   disable_warnings=1
    Set Global Variable     ${clamp_session}      clamp
    ${response_query}=    Get Request    ${clamp_session}   /restservices/clds/v2/policies/list
    Should Be Equal As Strings  ${response_query.status_code}     200
    Should Contain   ${response_query.text}   ${reponse_pdp_group}
/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

INSERT INTO automationcomposition (instanceid, compositionid, compositiontargetid, deploystate, description, lockstate,
                                   name, statechangeresult, version, lastmsg, phase, substate, revisionId)
VALUES ('dd36aaa4-580f-4193-a52b-37c3a955b11a', 'd30b8017-4d64-4693-84d7-de9c4226b9f8', NULL, 0,
        'Demo automation composition instance 0', 0, 'Test-Instance-Migration-From', 0, '2.0.1',
        '2025-07-16 09:53:15.512496', NULL, 0, 'e999d17c-5b80-476f-96cc-1acfb38194ee');

INSERT INTO automationcompositionelement (elementid, definition_name, definition_version, deploystate, description,
                                          instanceid, lockstate, message, operationalstate, outproperties,
                                          participantid, properties, usestate, substate, stage)
VALUES ('989c62b3-8918-41b9-a747-d21eb79c6c36', 'onap.policy.clamp.ac.element.Sim_SinkAutomationCompositionElement',
        '1.2.3', 0, 'Sink Automation Composition Element for the Demo', 'dd36aaa4-580f-4193-a52b-37c3a955b11a', 0,
        'Deployed', NULL, '{"InternalState":"DEPLOYED"}', '101c62b3-8918-41b9-a747-d21eb79c6c90',
        '{"deployTimeoutMs":200000,"undeployTimeoutMs":150000,"updateTimeoutMs":200000,"migrateTimeoutMs":200000,"deleteTimeoutMs":100000,"baseUrl":"ENCRYPTED:ZuDP803svc1ThgeVcYeQvA0kL8gpz2Qzg9unk3zm+XSi8RSTlDQ2qgSzROz7QOit","httpHeaders":{"Content-Type":"application/json","Authorization":"Basic YWNtVXNlcjp6YiFYenRHMzQ="},"configurationEntities":[{"configurationEntityId":{"name":"onap.policy.clamp.ac.sink","version":"1.0.0"},"restSequence":[{"restRequestId":{"name":"request3","version":"1.0.1"},"httpMethod":"POST","path":"/onap/policy/clamp/acelement/v2/activate","body":"{ \"receiverId\": { \"name\": \"onap.policy.clamp.ac.sink\", \"version\": \"1.0.0\" }, \"timerMs\": 20000, \"elementType\": \"SINK\", \"topicParameterGroup\": { \"server\": \"message-router\", \"listenerTopic\": \"POLICY_UPDATE_MSG\", \"publisherTopic\": \"AC_ELEMENT_MSG\", \"fetchTimeout\": 15000, \"topicCommInfrastructure\": \"dmaap\" } }","expectedResponse":201}],"myParameterToUpdate":"text"}]}',
        NULL, 0, NULL);

INSERT INTO automationcompositionelement (elementid, definition_name, definition_version, deploystate, description,
                                          instanceid, lockstate, message, operationalstate, outproperties,
                                          participantid, properties, usestate, substate, stage)
VALUES ('989c62b3-8918-41b9-a747-d21eb79c6c34', 'onap.policy.clamp.ac.element.Sim_StarterAutomationCompositionElement',
        '1.2.3', 0, 'Starter Automation Composition Element for the Demo', 'dd36aaa4-580f-4193-a52b-37c3a955b11a', 0,
        'Deployed', NULL, '{"InternalState":"DEPLOYED"}', '101c62b3-8918-41b9-a747-d21eb79c6c90',
        '{"deployTimeoutMs":200000,"undeployTimeoutMs":150000,"updateTimeoutMs":200000,"migrateTimeoutMs":200000,"deleteTimeoutMs":100000,"baseUrl":"ENCRYPTED:1IPD8QRnRhnFlUfrHIoRRuzPYHV0z88DhwXYa+KhZqv6iUh1JiVoN28oAIYFq1i6","httpHeaders":{"Content-Type":"application/json","Authorization":"Basic YWNtVXNlcjp6YiFYenRHMzQ="},"configurationEntities":[{"configurationEntityId":{"name":"onap.policy.clamp.ac.starter","version":"1.0.0"},"restSequence":[{"restRequestId":{"name":"request1","version":"1.0.1"},"httpMethod":"POST","path":"/onap/policy/clamp/acelement/v2/activate","body":"{ \"receiverId\": { \"name\": \"onap.policy.clamp.ac.startertobridge\", \"version\": \"1.0.0\" }, \"timerMs\": 20000, \"elementType\": \"STARTER\", \"topicParameterGroup\": { \"server\": \"message-router:3904\", \"listenerTopic\": \"POLICY_UPDATE_MSG\", \"publisherTopic\": \"AC_ELEMENT_MSG\", \"fetchTimeout\": 15000, \"topicCommInfrastructure\": \"dmaap\" } }","expectedResponse":201}],"myParameterToUpdate":"text"}]}',
        NULL, 0, NULL);

INSERT INTO automationcompositionelement (elementid, definition_name, definition_version, deploystate, description,
                                          instanceid, lockstate, message, operationalstate, outproperties,
                                          participantid, properties, usestate, substate, stage)
VALUES ('989c62b3-8918-41b9-a747-d21eb79c6c35', 'onap.policy.clamp.ac.element.Sim_BridgeAutomationCompositionElement',
        '1.2.3', 0, 'Bridge Automation Composition Element for the Demo', 'dd36aaa4-580f-4193-a52b-37c3a955b11a', 0,
        'Deployed', NULL, '{"InternalState":"DEPLOYED"}', '101c62b3-8918-41b9-a747-d21eb79c6c90',
        '{"deployTimeoutMs":200000,"undeployTimeoutMs":150000,"updateTimeoutMs":200000,"migrateTimeoutMs":200000,"deleteTimeoutMs":100000,"baseUrl":"ENCRYPTED:Q4bNY29WKbV4o/vi15DMyWnWla6lTTwbn5ZRfR1nspd5xL9vDvtl8FYBA7Sj6mHI","httpHeaders":{"Content-Type":"application/json","Authorization":"Basic YWNtVXNlcjp6YiFYenRHMzQ="},"configurationEntities":[{"configurationEntityId":{"name":"onap.policy.clamp.ac.bridge","version":"1.0.0"},"restSequence":[{"restRequestId":{"name":"request2","version":"1.0.1"},"httpMethod":"POST","path":"/onap/policy/clamp/acelement/v2/activate","body":"{ \"receiverId\": { \"name\": \"onap.policy.clamp.ac.bridgetosink\", \"version\": \"1.0.0\" }, \"timerMs\": 20000, \"elementType\": \"BRIDGE\", \"topicParameterGroup\": { \"server\": \"message-router:3904\", \"listenerTopic\": \"POLICY_UPDATE_MSG\", \"publisherTopic\": \"AC_ELEMENT_MSG\", \"fetchTimeout\": 15000, \"topicCommInfrastructure\": \"dmaap\" } }","expectedResponse":201}],"myParameterToUpdate":"text"}]}',
        NULL, 0, NULL);

/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.models.acm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;

class AcmUtilsTest {

    private static final String POLICY_AUTOMATION_COMPOSITION_ELEMENT =
            "org.onap.policy.clamp.acm.PolicyAutomationCompositionElement";
    private static final String PARTICIPANT_AUTOMATION_COMPOSITION_ELEMENT = "org.onap.policy.clamp.acm.Participant";
    private static final String TOSCA_TEMPLATE_YAML = "clamp/acm/pmsh/funtional-pmsh-usecase.yaml";
    private static final String AC_INSTANTIATION_JSON = "src/test/resources/json/AutomationComposition.json";
    public static final String AUTOMATION_COMPOSITION_ELEMENT =
            "org.onap.policy.clamp.acm.AutomationCompositionElement";
    public static final String AUTOMATION_COMPOSITION_NODE_TYPE = "org.onap.policy.clamp.acm.AutomationComposition";

    @Test
    void testCheckIfNodeTemplateIsAutomationCompositionElement() {
        var serviceTemplate = CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);
        var nodeTemplate = new ToscaNodeTemplate();
        nodeTemplate.setType(AUTOMATION_COMPOSITION_ELEMENT);
        assertThat(AcmUtils.checkIfNodeTemplateIsAutomationCompositionElement(nodeTemplate, serviceTemplate,
                AUTOMATION_COMPOSITION_ELEMENT)).isTrue();

        nodeTemplate.setType(POLICY_AUTOMATION_COMPOSITION_ELEMENT);
        assertThat(AcmUtils.checkIfNodeTemplateIsAutomationCompositionElement(nodeTemplate, serviceTemplate,
                AUTOMATION_COMPOSITION_ELEMENT)).isTrue();

        nodeTemplate.setType(PARTICIPANT_AUTOMATION_COMPOSITION_ELEMENT);
        assertThat(AcmUtils.checkIfNodeTemplateIsAutomationCompositionElement(nodeTemplate, serviceTemplate,
                AUTOMATION_COMPOSITION_ELEMENT)).isFalse();
    }

    @Test
    void testPrepareParticipantPriming() {
        var serviceTemplate = CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);

        var acElements =
                AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate, AUTOMATION_COMPOSITION_ELEMENT);
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setState(AcTypeState.PRIMED);
        acDefinition.setServiceTemplate(serviceTemplate);
        acDefinition.setCompositionId(UUID.randomUUID());
        acDefinition.setElementStateMap(AcmUtils.createElementStateMap(acElements, AcTypeState.PRIMED));
        acDefinition.getElementStateMap().values().forEach(nodeTemplateState
                -> nodeTemplateState.setOutProperties(Map.of("outProperty", "testProperty")));

        Map<ToscaConceptIdentifier, UUID> map = new HashMap<>();
        var participantId = UUID.randomUUID();
        assertThatThrownBy(() -> AcmUtils.prepareParticipantPriming(acElements, map, acDefinition)).hasMessageMatching(
                "Element Type org.onap.policy.clamp.acm.PolicyAutomationCompositionElement 1.0.0 not supported");
        map.put(new ToscaConceptIdentifier("org.onap.policy.clamp.acm.PolicyAutomationCompositionElement", "1.0.0"),
                participantId);
        map.put(new ToscaConceptIdentifier("org.onap.policy.clamp.acm.K8SMicroserviceAutomationCompositionElement",
                "1.0.0"), participantId);
        map.put(new ToscaConceptIdentifier("org.onap.policy.clamp.acm.HttpAutomationCompositionElement", "1.0.0"),
                participantId);
        var result = AcmUtils.prepareParticipantPriming(acElements, map, acDefinition);
        assertThat(result).isNotEmpty().hasSize(1);
        for (var participantDefList : result) {
            assertTrue(participantDefList.getAutomationCompositionElementDefinitionList().stream()
                    .allMatch(element -> "testProperty".equals(element.getOutProperties().get("outProperty"))));
        }
    }

    @Test
    void testValidateAutomationComposition() {
        var doc = new DocToscaServiceTemplate(CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML));
        var automationComposition = CommonTestData.getJsonObject(AC_INSTANTIATION_JSON, AutomationComposition.class);
        var result = AcmUtils.validateAutomationComposition(automationComposition, doc.toAuthorative(),
                AUTOMATION_COMPOSITION_NODE_TYPE, 0);
        assertTrue(result.isValid());

        var element = automationComposition.getElements().values().iterator().next();
        automationComposition.getElements().remove(element.getId());
        result = AcmUtils.validateAutomationComposition(automationComposition, doc.toAuthorative(),
                AUTOMATION_COMPOSITION_NODE_TYPE, 0);
        assertFalse(result.isValid());
        assertThat(result.getMessage()).contains("not matching");
    }

    @Test
    void testNotValidateAutomationComposition() {
        var automationComposition = getDummyAutomationComposition();
        var toscaServiceTemplate = getDummyToscaServiceTemplate();
        var result = AcmUtils.validateAutomationComposition(automationComposition,
                toscaServiceTemplate, AUTOMATION_COMPOSITION_NODE_TYPE, 0);
        assertNotNull(result);
        assertFalse(result.isValid());

        Map<String, ToscaNodeTemplate> nodeTemplates = new HashMap<>();
        var nodeTemplate = new ToscaNodeTemplate();
        nodeTemplate.setType("org.onap.policy.clamp.acm.AutomationComposition");
        nodeTemplates.put("org.onap.dcae.acm.DCAEMicroserviceAutomationCompositionParticipant", nodeTemplate);
        toscaServiceTemplate.getToscaTopologyTemplate().setNodeTemplates(nodeTemplates);
        result = AcmUtils.validateAutomationComposition(automationComposition, toscaServiceTemplate,
                AUTOMATION_COMPOSITION_NODE_TYPE, 0);
        assertFalse(result.isValid());

        var doc = new DocToscaServiceTemplate(CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML));
        result = AcmUtils.validateAutomationComposition(automationComposition, doc.toAuthorative(),
                AUTOMATION_COMPOSITION_NODE_TYPE, 0);
        assertFalse(result.isValid());
    }

    @Test
    void testCreateAcElementDeploy() {
        var element = getDummyAutomationComposition().getElements().values().iterator().next();
        var result = AcmUtils.createAcElementDeploy(element, DeployOrder.DEPLOY);
        assertEquals(DeployOrder.DEPLOY, result.getOrderedState());
        assertEquals(element.getId(), result.getId());
        assertEquals(element.getDefinition(), result.getDefinition());
    }

    @Test
    void testCreateAcElementDeployList() {
        var automationComposition = getDummyAutomationComposition();
        var result = AcmUtils.createParticipantDeployList(automationComposition, DeployOrder.DEPLOY);
        assertThat(result).hasSameSizeAs(automationComposition.getElements().values());
        for (var participantDeploy : result) {
            for (var element : participantDeploy.getAcElementList()) {
                assertEquals(DeployOrder.DEPLOY, element.getOrderedState());
            }
        }
    }

    @Test
    void testCreateAcElementRestart() {
        var element = getDummyAutomationComposition().getElements().values().iterator().next();
        var result = AcmUtils.createAcElementRestart(element);
        assertEquals(element.getId(), result.getId());
        assertEquals(element.getDefinition(), result.getDefinition());
        assertEquals(element.getDeployState(), result.getDeployState());
        assertEquals(element.getLockState(), result.getLockState());
        assertEquals(element.getOperationalState(), result.getOperationalState());
        assertEquals(element.getUseState(), result.getUseState());
        assertEquals(element.getProperties(), result.getProperties());
        assertEquals(element.getOutProperties(), result.getOutProperties());
    }

    @Test
    void testValidatedMessage() {
        var message = "completed";
        assertEquals(message, AcmUtils.validatedMessage(message));

        var serviceTemplate = CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);
        message = serviceTemplate.toString();
        assertEquals(message.substring(0, 255), AcmUtils.validatedMessage(message));
    }

    private AutomationComposition getDummyAutomationComposition() {
        var automationComposition = new AutomationComposition();
        automationComposition.setCompositionId(UUID.randomUUID());
        Map<UUID, AutomationCompositionElement> map = new LinkedHashMap<>();
        var element = CommonTestData.getJsonObject(
                "src/test/resources/json/AutomationCompositionElementNoOrderedState.json",
                AutomationCompositionElement.class);
        map.put(UUID.randomUUID(), element);
        automationComposition.setElements(map);
        return automationComposition;
    }

    private ToscaServiceTemplate getDummyToscaServiceTemplate() {
        var toscaServiceTemplate = new ToscaServiceTemplate();
        var policyTypes = getDummyPolicyTypesMap();
        toscaServiceTemplate.setPolicyTypes(policyTypes);

        var dataTypes = getDummyToscaDataTypeMap();
        dataTypes.put("onap.datatypes.ToscaConceptIdentifier", new ToscaDataType());
        toscaServiceTemplate.setDataTypes(dataTypes);

        var toscaTopologyTemplate = new ToscaTopologyTemplate();
        Map<String, ToscaPolicy> policy = new HashMap<>();
        toscaTopologyTemplate.setPolicies(List.of(policy));
        var nodeTemplates = getDummyNodeTemplates();
        toscaTopologyTemplate.setNodeTemplates(nodeTemplates);

        toscaServiceTemplate.setToscaTopologyTemplate(toscaTopologyTemplate);
        toscaServiceTemplate.setDerivedFrom("tosca.nodetypes.Root");
        toscaServiceTemplate.setDescription("description");
        toscaServiceTemplate.setMetadata(null);
        toscaServiceTemplate.setName("name");
        toscaServiceTemplate.setToscaDefinitionsVersion("1.0.0");
        toscaServiceTemplate.setVersion("1.0.1");
        return toscaServiceTemplate;
    }

    private Map<String, ToscaPolicyType> getDummyPolicyTypesMap() {
        return Map.of("onap.policies.Match", new ToscaPolicyType());
    }

    private Map<String, ToscaDataType> getDummyToscaDataTypeMap() {
        Map<String, ToscaDataType> dataTypes = new HashMap<>();
        dataTypes.put("onap.datatypes.ToscaConceptIdentifier", new ToscaDataType());
        return dataTypes;
    }

    private Map<String, ToscaNodeTemplate> getDummyNodeTemplates() {
        Map<String, ToscaNodeTemplate> nodeTemplates = new HashMap<>();
        var nodeTemplate = new ToscaNodeTemplate();
        nodeTemplate.setType(AUTOMATION_COMPOSITION_ELEMENT);
        nodeTemplates.put("org.onap.dcae.acm.DCAEMicroserviceAutomationCompositionParticipant", nodeTemplate);
        return nodeTemplates;
    }

    @Test
    void testcreateAcRestart() {
        var automationComposition = getDummyAutomationComposition();
        automationComposition.setInstanceId(UUID.randomUUID());
        var element = automationComposition.getElements().values().iterator().next();
        var secondElement = new AutomationCompositionElement(element);
        secondElement.setParticipantId(UUID.randomUUID());
        secondElement.setId(UUID.randomUUID());
        automationComposition.getElements().put(secondElement.getId(), secondElement);
        var result = AcmUtils.createAcRestart(automationComposition, element.getParticipantId());
        assertEquals(result.getAutomationCompositionId(), automationComposition.getInstanceId());
        assertThat(result.getAcElementList()).hasSize(1);
    }

    @Test
    void testPrepareParticipantRestarting() {
        var serviceTemplate = CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setElementStateMap(Map.of());
        acmDefinition.setServiceTemplate(serviceTemplate);
        var acElements = AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate, AUTOMATION_COMPOSITION_ELEMENT);
        acmDefinition.setElementStateMap(AcmUtils.createElementStateMap(acElements, AcTypeState.COMMISSIONED));
        acmDefinition.getElementStateMap()
                .values().forEach(element -> element.setParticipantId(UUID.randomUUID()));
        var participantId = UUID.randomUUID();
        var result = AcmUtils.prepareParticipantRestarting(participantId, acmDefinition,
                AUTOMATION_COMPOSITION_ELEMENT);
        assertThat(result).isEmpty();

        participantId = acmDefinition.getElementStateMap().values().iterator().next().getParticipantId();
        result = AcmUtils.prepareParticipantRestarting(participantId, acmDefinition,
                AUTOMATION_COMPOSITION_ELEMENT);
        assertThat(result).hasSize(1);
    }

    @Test
    void testRecursiveMergeMap() {
        var oldProperties = """
            chart:
              chartId:
                name: acelement
                version: 0.1.0
              namespace: default
              releaseName: acm-starter
              podName: acm-starter
            """;

        var newProperties = """
            chart:
              releaseName: acm-starter-new
              podName: null
            """;

        Map<String, Object> map = CommonTestData.getObject(oldProperties, Map.class);
        Map<String, Object> mapMigrate = CommonTestData.getObject(newProperties, Map.class);

        AcmUtils.recursiveMerge(map, mapMigrate);
        assertEquals("default", ((Map<String, Object>) map.get("chart")).get("namespace"));
        assertEquals("acm-starter-new", ((Map<String, Object>) map.get("chart")).get("releaseName"));
        assertNotNull(((Map<String, Object>) map.get("chart")).get("chartId"));
        assertNull(((Map<String, Object>) map.get("chart")).get("podName"));
    }

    @Test
    void testRecursiveMergeList() {
        var oldProperties = """
            baseUrl: http://{{address}}:30800
            httpHeaders:
              Content-Type: application/json
              Authorization: Basic YWNtVXNlcjp6YiFYenRHMzQ=
            configurationEntities:
              - configurationEntityId:
                  name: onap.policy.clamp.ac.starter
                  version: 1.0.0
                restSequence:
                  - restRequestId:
                      name: request1
                      version: 1.0.1
                myParameterToUpdate: 9
                myParameterToRemove: 8
            """;

        var newProperties = """
            configurationEntities:
              - myParameterToUpdate: "90"
                myParameterToRemove: null
                myParameter: "I am new"
            """;

        Map<String, Object> map = CommonTestData.getObject(oldProperties, Map.class);
        Map<String, Object> mapMigrate = CommonTestData.getObject(newProperties, Map.class);

        AcmUtils.recursiveMerge(map, mapMigrate);
        assertEquals("http://{{address}}:30800", map.get("baseUrl"));
        assertEquals("application/json", ((Map<String, Object>) map.get("httpHeaders")).get("Content-Type"));
        var configurationEntities = (List<Object>) map.get("configurationEntities");
        var subMap = (Map<String, Object>) configurationEntities.get(0);
        assertEquals("onap.policy.clamp.ac.starter",
                ((Map<String, Object>) subMap.get("configurationEntityId")).get("name"));
        assertThat((List<Object>) subMap.get("restSequence")).isNotEmpty();
        assertEquals("90", subMap.get("myParameterToUpdate"));
        assertNull(subMap.get("myParameterToRemove"));
        assertEquals("I am new", subMap.get("myParameter"));
    }

    @Test
    void testCopyMap() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> subMap = new HashMap<>();
        subMap.put("test", "value");
        map.put("sub", subMap);
        var result = AcmUtils.cloneMap(map);
        var subMap2 = (Map<String, Object>) result.get("sub");
        subMap2.put("test", "value2");
        assertNotEquals(subMap.get("test"), subMap2.get("test"));
    }
}

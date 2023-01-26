/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUpdates;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate;
import org.onap.policy.common.utils.coder.StandardCoder;
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

    @Test
    void testCommonUtilsParticipantUpdate() {
        var acElement = new AutomationCompositionElement();
        List<ParticipantUpdates> participantUpdates = new ArrayList<>();
        assertThat(participantUpdates).isEmpty();

        AcmUtils.prepareParticipantUpdate(acElement, participantUpdates);
        assertThat(participantUpdates).isNotEmpty();
        assertEquals(acElement, participantUpdates.get(0).getAutomationCompositionElementList().get(0));

        AcmUtils.prepareParticipantUpdate(acElement, participantUpdates);
        var participantId = CommonTestData.getParticipantId();
        assertNotEquals(participantId, participantUpdates.get(0).getParticipantId());

        acElement.setParticipantId(participantId);
        AcmUtils.prepareParticipantUpdate(acElement, participantUpdates);
        assertEquals(participantId, participantUpdates.get(1).getParticipantId());
    }

    @Test
    void testCheckIfNodeTemplateIsAutomationCompositionElement() {
        var serviceTemplate = CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);
        var nodeTemplate = new ToscaNodeTemplate();
        nodeTemplate.setType(AcmUtils.AUTOMATION_COMPOSITION_ELEMENT);
        assertThat(AcmUtils.checkIfNodeTemplateIsAutomationCompositionElement(nodeTemplate, serviceTemplate)).isTrue();

        nodeTemplate.setType(POLICY_AUTOMATION_COMPOSITION_ELEMENT);
        assertThat(AcmUtils.checkIfNodeTemplateIsAutomationCompositionElement(nodeTemplate, serviceTemplate)).isTrue();

        nodeTemplate.setType(PARTICIPANT_AUTOMATION_COMPOSITION_ELEMENT);
        assertThat(AcmUtils.checkIfNodeTemplateIsAutomationCompositionElement(nodeTemplate, serviceTemplate)).isFalse();
    }

    @Test
    void testPrepareParticipantPriming() {
        var serviceTemplate = CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);

        var acElements = AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate);
        Map<ToscaConceptIdentifier, UUID> map = new HashMap<>();
        var participantId = UUID.randomUUID();
        assertThatThrownBy(() -> AcmUtils.prepareParticipantPriming(acElements, map)).hasMessageMatching(
                "Element Type org.onap.policy.clamp.acm.PolicyAutomationCompositionElement 1.0.1 not supported");
        map.put(new ToscaConceptIdentifier("org.onap.policy.clamp.acm.PolicyAutomationCompositionElement", "1.0.1"),
                participantId);
        map.put(new ToscaConceptIdentifier("org.onap.policy.clamp.acm.K8SMicroserviceAutomationCompositionElement",
                "1.0.1"), participantId);
        map.put(new ToscaConceptIdentifier("org.onap.policy.clamp.acm.HttpAutomationCompositionElement", "1.0.1"),
                participantId);
        var result = AcmUtils.prepareParticipantPriming(acElements, map);
        assertThat(result).isNotEmpty().hasSize(1);
    }

    @Test
    void testCommonUtilsServiceTemplate() {
        var acElement = new AutomationCompositionElement();
        var toscaServiceTemplate = getDummyToscaServiceTemplate();
        AcmUtils.setAcPolicyInfo(acElement, toscaServiceTemplate);
        assertEquals(getDummyToscaDataTypeMap(), acElement.getToscaServiceTemplateFragment().getDataTypes());
    }

    @Test
    void testSetServiceTemplatePolicyInfoWithNullInfo() {
        var toscaServiceTemplate = getDummyToscaServiceTemplate();
        toscaServiceTemplate.setPolicyTypes(null);
        toscaServiceTemplate.getToscaTopologyTemplate().setPolicies(null);
        AutomationCompositionElement acElement = new AutomationCompositionElement();
        AcmUtils.setAcPolicyInfo(new AutomationCompositionElement(), toscaServiceTemplate);
        assertNull(acElement.getToscaServiceTemplateFragment());
    }

    @Test
    void testValidateAutomationComposition() {
        var automationComposition = getDummyAutomationComposition();
        var toscaServiceTemplate = getDummyToscaServiceTemplate();
        var result = AcmUtils.validateAutomationComposition(automationComposition, toscaServiceTemplate);
        assertNotNull(result);
        assertFalse(result.isValid());

        Map<String, ToscaNodeTemplate> nodeTemplates = new HashMap<>();
        var nodeTemplate = new ToscaNodeTemplate();
        nodeTemplate.setType("org.onap.policy.clamp.acm.AutomationComposition");
        nodeTemplates.put("org.onap.dcae.acm.DCAEMicroserviceAutomationCompositionParticipant", nodeTemplate);
        toscaServiceTemplate.getToscaTopologyTemplate().setNodeTemplates(nodeTemplates);
        result = AcmUtils.validateAutomationComposition(automationComposition, toscaServiceTemplate);
        assertFalse(result.isValid());

        var doc = new DocToscaServiceTemplate(CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML));
        result = AcmUtils.validateAutomationComposition(automationComposition, doc.toAuthorative());
        assertFalse(result.isValid());
    }

    private AutomationComposition getDummyAutomationComposition() {
        var automationComposition = new AutomationComposition();
        automationComposition.setCompositionId(UUID.randomUUID());
        Map<UUID, AutomationCompositionElement> map = new LinkedHashMap<>();
        try {
            var element = new StandardCoder().decode(
                new File("src/test/resources/json/AutomationCompositionElementNoOrderedState.json"),
                AutomationCompositionElement.class);
            map.put(UUID.randomUUID(), element);
        } catch (Exception e) {
            fail("Cannot read or decode " + e.getMessage());
            return null;
        }
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
        Map<String, ToscaPolicyType> policyTypes = new HashMap<>();
        policyTypes.put("onap.policies.Match", new ToscaPolicyType());
        return policyTypes;
    }

    private Map<String, ToscaDataType> getDummyToscaDataTypeMap() {
        Map<String, ToscaDataType> dataTypes = new HashMap<>();
        dataTypes.put("onap.datatypes.ToscaConceptIdentifier", new ToscaDataType());
        return dataTypes;
    }

    private Map<String, ToscaNodeTemplate> getDummyNodeTemplates() {
        Map<String, ToscaNodeTemplate> nodeTemplates = new HashMap<>();
        var nodeTemplate = new ToscaNodeTemplate();
        nodeTemplate.setType("org.onap.policy.clamp.acm.AutomationCompositionElement");
        nodeTemplates.put("org.onap.dcae.acm.DCAEMicroserviceAutomationCompositionParticipant", nodeTemplate);
        return nodeTemplates;
    }
}

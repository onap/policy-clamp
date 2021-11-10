/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantUpdates;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;

class CommonUtilsTest {

    private ToscaConceptIdentifier id = new ToscaConceptIdentifier("id", "1.0.0");
    private ToscaConceptIdentifier idNode = new ToscaConceptIdentifier(
            "org.onap.dcae.controlloop.DCAEMicroserviceControlLoopParticipant", "0.0.0");

    @Test
    void testCommonUtilsParticipantUpdate() {
        var clElement = new ControlLoopElement();
        List<ParticipantUpdates> participantUpdates = new ArrayList<>();
        assertThat(participantUpdates).isEmpty();

        CommonUtils.prepareParticipantUpdate(clElement, participantUpdates);
        assertThat(participantUpdates).isNotEmpty();
        assertEquals(clElement, participantUpdates.get(0).getControlLoopElementList().get(0));

        CommonUtils.prepareParticipantUpdate(clElement, participantUpdates);
        assertNotEquals(id, participantUpdates.get(0).getParticipantId());

        clElement.setParticipantId(id);
        clElement.setParticipantType(id);
        CommonUtils.prepareParticipantUpdate(clElement, participantUpdates);
        assertEquals(id, participantUpdates.get(1).getParticipantId());
    }

    @Test
    void testCommonUtilsServiceTemplate() {
        var clElement = new ControlLoopElement();
        var toscaServiceTemplate = getDummyToscaServiceTemplate();
        CommonUtils.setServiceTemplatePolicyInfo(clElement, toscaServiceTemplate);
        assertEquals(getDummyToscaDataTypeMap(), clElement.getToscaServiceTemplateFragment().getDataTypes());
    }

    @Test
    void testCommonUtilsDefinitionUpdate() {
        var toscaServiceTemplate = getDummyToscaServiceTemplate();
        List<ParticipantDefinition> participantDefinitionUpdates = new ArrayList<>();
        assertThat(participantDefinitionUpdates).isEmpty();

        checkParticipantDefinitionUpdate(toscaServiceTemplate, participantDefinitionUpdates);
        assertThat(participantDefinitionUpdates).isNotEmpty();
        assertEquals(id, participantDefinitionUpdates.get(0).getParticipantType());

        checkParticipantDefinitionUpdate(toscaServiceTemplate, participantDefinitionUpdates);
        assertEquals(idNode, participantDefinitionUpdates.get(0)
                .getControlLoopElementDefinitionList().get(0)
                .getClElementDefinitionId());
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
        nodeTemplate.setType("org.onap.policy.clamp.controlloop.ControlLoopElement");
        nodeTemplates.put("org.onap.dcae.controlloop.DCAEMicroserviceControlLoopParticipant", nodeTemplate);
        return nodeTemplates;
    }

    private void checkParticipantDefinitionUpdate(
            ToscaServiceTemplate toscaServiceTemplate,
            List<ParticipantDefinition> participantDefinitionUpdates) {

        for (Map.Entry<String, ToscaNodeTemplate> toscaInputEntry : toscaServiceTemplate.getToscaTopologyTemplate()
                .getNodeTemplates().entrySet()) {
            if (ParticipantUtils.checkIfNodeTemplateIsControlLoopElement(toscaInputEntry.getValue(),
                    toscaServiceTemplate)) {
                CommonUtils.prepareParticipantDefinitionUpdate(id, toscaInputEntry.getKey(),
                        toscaInputEntry.getValue(), participantDefinitionUpdates, null);
            }
        }
    }
}

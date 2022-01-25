/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.messages.dmaap.participant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageUtils.assertSerializable;
import static org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageUtils.removeVariableFields;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatistics;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatisticsList;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionStatistics;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;

class ParticipantStatusTest {

    @Test
    void testCopyConstructor() throws CoderException {
        assertThatThrownBy(() -> new ParticipantStatus(null)).isInstanceOf(NullPointerException.class);

        final ParticipantStatus orig = new ParticipantStatus();

        // verify with null values
        assertEquals(removeVariableFields(orig.toString()),
                removeVariableFields(new ParticipantStatus(orig).toString()));

        // verify with all values
        ToscaConceptIdentifier id = new ToscaConceptIdentifier("id", "1.2.3");
        orig.setAutomationCompositionId(id);
        orig.setParticipantId(id);
        ToscaConceptIdentifier type = new ToscaConceptIdentifier("type", "2.3.4");
        orig.setParticipantType(type);
        orig.setMessageId(UUID.randomUUID());
        orig.setState(ParticipantState.ACTIVE);
        orig.setHealthStatus(ParticipantHealthStatus.HEALTHY);
        orig.setTimestamp(Instant.ofEpochMilli(3000));

        AutomationCompositionInfo acInfo = getAutomationCompositionInfo(id);
        orig.setAutomationCompositionInfoList(List.of(acInfo));

        ParticipantDefinition participantDefinitionUpdate = new ParticipantDefinition();
        participantDefinitionUpdate.setParticipantId(id);
        participantDefinitionUpdate.setParticipantType(type);
        AutomationCompositionElementDefinition acDefinition = getAcElementDefinition(id);
        participantDefinitionUpdate.setAutomationCompositionElementDefinitionList(List.of(acDefinition));
        orig.setParticipantDefinitionUpdates(List.of(participantDefinitionUpdate));

        assertEquals(removeVariableFields(orig.toString()),
                removeVariableFields(new ParticipantStatus(orig).toString()));

        assertSerializable(orig, ParticipantStatus.class);
    }

    private AutomationCompositionInfo getAutomationCompositionInfo(ToscaConceptIdentifier id) {
        AutomationCompositionInfo acInfo = new AutomationCompositionInfo();
        acInfo.setState(AutomationCompositionState.PASSIVE2RUNNING);
        acInfo.setAutomationCompositionId(id);

        AutomationCompositionStatistics acStatistics = new AutomationCompositionStatistics();
        acStatistics.setAutomationCompositionId(id);
        acStatistics.setAverageExecutionTime(12345);
        acStatistics.setEventCount(12345);
        acStatistics.setLastEnterTime(12345);
        acStatistics.setLastExecutionTime(12345);
        acStatistics.setLastStart(12345);
        acStatistics.setTimeStamp(Instant.ofEpochMilli(3000));
        acStatistics.setUpTime(12345);
        AcElementStatisticsList acElementStatisticsList = new AcElementStatisticsList();
        AcElementStatistics acElementStatistics = new AcElementStatistics();
        acElementStatistics.setParticipantId(new ToscaConceptIdentifier("defName", "0.0.1"));
        acElementStatistics.setTimeStamp(Instant.now());
        acElementStatisticsList.setAcElementStatistics(List.of(acElementStatistics));
        acStatistics.setAcElementStatisticsList(acElementStatisticsList);

        acInfo.setAutomationCompositionStatistics(acStatistics);
        return acInfo;
    }

    private AutomationCompositionElementDefinition getAcElementDefinition(ToscaConceptIdentifier id) {
        ToscaNodeTemplate toscaNodeTemplate = new ToscaNodeTemplate();
        toscaNodeTemplate.setName("nodeTemplate");
        toscaNodeTemplate.setDerivedFrom("parentNodeTemplate");
        toscaNodeTemplate.setDescription("Description of nodeTemplate");
        toscaNodeTemplate.setVersion("1.2.3");

        AutomationCompositionElementDefinition acDefinition = new AutomationCompositionElementDefinition();
        acDefinition.setAcElementDefinitionId(id);
        acDefinition.setAutomationCompositionElementToscaNodeTemplate(toscaNodeTemplate);

        ToscaProperty property = new ToscaProperty();
        property.setName("test");
        property.setType("testType");
        Map<String, ToscaProperty> commonPropertiesMap = Map.of("Prop1", property);
        acDefinition.setCommonPropertiesMap(commonPropertiesMap);
        return acDefinition;
    }
}

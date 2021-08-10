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

package org.onap.policy.clamp.controlloop.models.messages.dmaap.participant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageUtils.removeVariableFields;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatisticsList;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopInfo;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;

class ParticipantStatusTest {

    @Test
    void testCopyConstructor() {
        assertThatThrownBy(() -> new ParticipantStatus(null)).isInstanceOf(NullPointerException.class);

        final ParticipantStatus orig = new ParticipantStatus();

        // verify with null values
        assertEquals(removeVariableFields(orig.toString()),
                removeVariableFields(new ParticipantStatus(orig).toString()));

        // verify with all values
        ToscaConceptIdentifier id = new ToscaConceptIdentifier("id", "1.2.3");
        orig.setControlLoopId(id);
        orig.setParticipantId(id);
        orig.setParticipantType(id);
        orig.setMessageId(UUID.randomUUID());
        orig.setState(ParticipantState.ACTIVE);
        orig.setHealthStatus(ParticipantHealthStatus.HEALTHY);
        orig.setTimestamp(Instant.ofEpochMilli(3000));

        ControlLoopInfo clInfo = getControlLoopInfo(id);
        orig.setControlLoopInfoMap(Map.of(id, clInfo));

        ControlLoopElementDefinition clDefinition = getClElementDefinition();
        Map<ToscaConceptIdentifier, ControlLoopElementDefinition> clElementDefinitionMap = Map.of(id, clDefinition);
        Map<ToscaConceptIdentifier, Map<ToscaConceptIdentifier, ControlLoopElementDefinition>>
            participantDefinitionUpdateMap = Map.of(id, clElementDefinitionMap);
        orig.setParticipantDefinitionUpdateMap(participantDefinitionUpdateMap);

        assertEquals(removeVariableFields(orig.toString()),
                removeVariableFields(new ParticipantStatus(orig).toString()));
    }

    private ControlLoopInfo getControlLoopInfo(ToscaConceptIdentifier id) {
        ControlLoopInfo clInfo = new ControlLoopInfo();
        clInfo.setState(ControlLoopState.PASSIVE2RUNNING);

        ControlLoopStatistics clStatistics = new ControlLoopStatistics();
        clStatistics.setControlLoopId(id);
        clStatistics.setAverageExecutionTime(12345);
        clStatistics.setEventCount(12345);
        clStatistics.setLastEnterTime(12345);
        clStatistics.setLastExecutionTime(12345);
        clStatistics.setLastStart(12345);
        clStatistics.setTimeStamp(Instant.ofEpochMilli(3000));
        clStatistics.setUpTime(12345);
        ClElementStatisticsList clElementStatisticsList = new ClElementStatisticsList();
        ClElementStatistics clElementStatistics = new ClElementStatistics();
        clElementStatistics.setParticipantId(new ToscaConceptIdentifier("defName", "0.0.1"));
        clElementStatistics.setTimeStamp(Instant.now());
        clElementStatisticsList.setClElementStatistics(List.of(clElementStatistics));
        clStatistics.setClElementStatisticsList(clElementStatisticsList);

        clInfo.setControlLoopStatistics(clStatistics);
        return clInfo;
    }

    private ControlLoopElementDefinition getClElementDefinition() {
        ToscaNodeTemplate toscaNodeTemplate = new ToscaNodeTemplate();
        toscaNodeTemplate.setName("nodeTemplate");
        toscaNodeTemplate.setDerivedFrom("parentNodeTemplate");
        toscaNodeTemplate.setDescription("Description of nodeTemplate");
        toscaNodeTemplate.setVersion("1.2.3");

        ControlLoopElementDefinition clDefinition = new ControlLoopElementDefinition();
        clDefinition.setControlLoopElementToscaNodeTemplate(toscaNodeTemplate);
        Map<String, String> commonPropertiesMap = Map.of("Prop1", "PropValue");
        clDefinition.setCommonPropertiesMap(commonPropertiesMap);
        return clDefinition;
    }
}

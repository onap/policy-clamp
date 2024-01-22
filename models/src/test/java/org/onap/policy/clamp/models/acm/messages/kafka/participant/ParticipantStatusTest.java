/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2024 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.messages.kafka.participant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageUtils.assertSerializable;
import static org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageUtils.removeVariableFields;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementInfo;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;

class ParticipantStatusTest {

    @Test
    void testCopyConstructor() throws CoderException {
        assertThatThrownBy(() -> new ParticipantStatus(null)).isInstanceOf(NullPointerException.class);

        final var orig = new ParticipantStatus();

        // verify with null values
        assertEquals(removeVariableFields(orig.toString()),
                removeVariableFields(new ParticipantStatus(orig).toString()));

        // verify with all values
        var automationCompositionId = UUID.randomUUID();
        var acElementId = UUID.randomUUID();
        orig.setAutomationCompositionId(automationCompositionId);
        var participantId = CommonTestData.getParticipantId();
        orig.setParticipantId(participantId);
        orig.setMessageId(UUID.randomUUID());
        orig.setState(ParticipantState.ON_LINE);
        orig.setTimestamp(Instant.ofEpochMilli(3000));

        var acInfo = getAutomationCompositionInfo(automationCompositionId, acElementId);
        orig.setAutomationCompositionInfoList(List.of(acInfo));

        var participantDefinitionUpdate = new ParticipantDefinition();
        participantDefinitionUpdate.setParticipantId(participantId);
        var acDefinition = getAcElementDefinition(new ToscaConceptIdentifier("id", "1.2.3"));
        participantDefinitionUpdate.setAutomationCompositionElementDefinitionList(List.of(acDefinition));
        orig.setParticipantDefinitionUpdates(List.of(participantDefinitionUpdate));

        assertEquals(removeVariableFields(orig.toString()),
                removeVariableFields(new ParticipantStatus(orig).toString()));

        assertSerializable(orig, ParticipantStatus.class);
    }

    private AutomationCompositionInfo getAutomationCompositionInfo(UUID id, UUID acElementId) {
        var acInfo = new AutomationCompositionInfo();
        acInfo.setDeployState(DeployState.DEPLOYED);
        acInfo.setLockState(LockState.LOCKED);
        acInfo.setAutomationCompositionId(id);

        var acInfoElement = new AutomationCompositionElementInfo();
        acInfoElement.setAutomationCompositionElementId(acElementId);
        acInfoElement.setDeployState(DeployState.DEPLOYED);
        acInfoElement.setLockState(LockState.LOCKED);
        acInfoElement.setOperationalState("DEFAULT");
        acInfoElement.setUseState("IDLE");
        acInfo.getElements().add(acInfoElement);
        return acInfo;
    }

    private AutomationCompositionElementDefinition getAcElementDefinition(ToscaConceptIdentifier id) {
        var toscaNodeTemplate = new ToscaNodeTemplate();
        toscaNodeTemplate.setName("nodeTemplate");
        toscaNodeTemplate.setDerivedFrom("parentNodeTemplate");
        toscaNodeTemplate.setDescription("Description of nodeTemplate");
        toscaNodeTemplate.setVersion("1.2.3");

        var acDefinition = new AutomationCompositionElementDefinition();
        acDefinition.setAcElementDefinitionId(id);
        acDefinition.setAutomationCompositionElementToscaNodeTemplate(toscaNodeTemplate);

        return acDefinition;
    }
}

/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageUtils.assertSerializable;
import static org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageUtils.removeVariableFields;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUpdates;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;

/**
 * Test the copy constructor.
 */
class AutomationCompositionUpdateTest {
    @Test
    void testCopyConstructor() throws CoderException {
        assertThatThrownBy(() -> new AutomationCompositionUpdate(null)).isInstanceOf(NullPointerException.class);

        var orig = new AutomationCompositionUpdate();
        // verify with all values
        orig.setAutomationCompositionId(UUID.randomUUID());
        orig.setParticipantId(null);
        orig.setMessageId(UUID.randomUUID());
        orig.setTimestamp(Instant.ofEpochMilli(3000));

        var acElement = new AutomationCompositionElement();
        acElement.setId(UUID.randomUUID());
        var id = new ToscaConceptIdentifier("id", "1.2.3");
        acElement.setDefinition(id);
        acElement.setDescription("Description");
        acElement.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
        acElement.setState(AutomationCompositionState.PASSIVE);

        var participantId = CommonTestData.getParticipantId();
        acElement.setParticipantId(participantId);
        acElement.setParticipantType(id);

        var property = new ToscaProperty();
        property.setName("test");
        property.setType("testType");
        var standardCoder = new StandardCoder();
        var json = standardCoder.encode(property);
        var propertiesMap = Map.of("Prop1", (Object) json);
        acElement.setProperties(propertiesMap);

        var participantUpdates = new ParticipantUpdates();
        participantUpdates.setParticipantId(participantId);
        participantUpdates.setAutomationCompositionElementList(List.of(acElement));
        orig.setParticipantUpdatesList(List.of(participantUpdates));

        var other = new AutomationCompositionUpdate(orig);

        assertEquals(removeVariableFields(orig.toString()), removeVariableFields(other.toString()));
        assertSerializable(orig, AutomationCompositionUpdate.class);
    }
}

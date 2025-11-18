/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageUtils.assertSerializable;
import static org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageUtils.removeVariableFields;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class PropertiesUpdateTest {

    @Test
    void testCopyConstructor() throws CoderException {
        var acElement = new AcElementDeploy();
        acElement.setId(UUID.randomUUID());
        var id = new ToscaConceptIdentifier("id", "1.2.3");
        acElement.setDefinition(id);
        var participantDeploy = new ParticipantDeploy();
        participantDeploy.setParticipantId(CommonTestData.getParticipantId());
        participantDeploy.setAcElementList(List.of(acElement));
        var orig = new PropertiesUpdate();
        orig.setParticipantUpdatesList(List.of(participantDeploy));
        orig.setCompositionId(UUID.randomUUID());
        orig.setRevisionIdComposition(UUID.randomUUID());
        orig.setAutomationCompositionId(UUID.randomUUID());
        orig.setRevisionIdInstance(UUID.randomUUID());

        var other = new PropertiesUpdate(orig);
        assertEquals(removeVariableFields(orig.toString()), removeVariableFields(other.toString()));
        assertSerializable(orig, PropertiesUpdate.class);
    }
}

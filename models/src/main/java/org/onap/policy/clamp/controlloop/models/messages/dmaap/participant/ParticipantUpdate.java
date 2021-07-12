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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * Class to represent the PARTICIPANT_UPDATE message that the control loop runtime sends to a participant.
 * CLAMP Runtime sends Control Loop Element Definitions and Common Parameter Values to Participants.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ParticipantUpdate extends ParticipantMessage {

    // A map with Participant ID as its key, and a map of ControlLoopElements as value.
    private Map<ToscaConceptIdentifier, Map<UUID, ControlLoopElementDefinition>>
            participantDefinitionUpdateMap = new LinkedHashMap<>();

    /**
     * Constructor for instantiating ParticipantUpdate class with message name.
     *
     */
    public ParticipantUpdate() {
        super(ParticipantMessageType.PARTICIPANT_UPDATE);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ParticipantUpdate(ParticipantUpdate source) {
        super(source);

        this.participantDefinitionUpdateMap = PfUtils.mapMap(source.participantDefinitionUpdateMap,
                clElementDefinitionMap -> PfUtils.mapMap(clElementDefinitionMap,
                        ControlLoopElementDefinition::new));
    }
}

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

    // Map with Participant ID as its key, each value on the map is a ControlLoopElementDefintionMap
    private Map<ToscaConceptIdentifier, Map<UUID, ControlLoopElementDefinition>>
            participantDefinitionUpdateMap = new LinkedHashMap<>();

    // List of ControlLoopElementDefinition values for a particular participant, keyed by its CLElementDefinitionID
    private Map<UUID, ControlLoopElementDefinition> controlLoopElementDefinitionMap =
            new LinkedHashMap<>();

    // A ControlLoopElementToscaServiceTemplate containing the definition of the Control Loop Element and a
    // CommonPropertiesMap with the values of the common property values for Control Loop Elements of this type
    ControlLoopElementDefinition controlLoopElementDefinition;

    // The definition of the Control Loop Element in TOSCA
    private ToscaServiceTemplate controlLoopElementToscaServiceTemplate;

    // A map indexed by the property name. Each map entry is the serialized value of the property,
    // which can be deserialized into an instance of the type of the property.
    private Map<String, String> commonPropertiesMap = new LinkedHashMap<>();

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
                UnaryOperator.identity());
        this.controlLoopElementDefinitionMap =
                PfUtils.mapMap(source.controlLoopElementDefinitionMap, UnaryOperator.identity());
        this.controlLoopElementDefinition = new ControlLoopElementDefinition(source.controlLoopElementDefinition);
        this.controlLoopElementToscaServiceTemplate =
                new ToscaServiceTemplate(source.controlLoopElementToscaServiceTemplate);
        this.commonPropertiesMap = PfUtils.mapMap(source.commonPropertiesMap, UnaryOperator.identity());

    }
}

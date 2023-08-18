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

package org.onap.policy.clamp.acm.runtime.util;

import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import java.util.UUID;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * Class to hold/create all parameters for test cases.
 *
 */
public class CommonTestData {
    private static final Coder CODER = new StandardCoder();
    public static final String TOSCA_SERVICE_TEMPLATE_YAML = "clamp/acm/pmsh/funtional-pmsh-usecase.yaml";

    /**
     * Gets the standard automation composition parameters.
     *
     * @param dbName the database name
     * @return the standard automation composition parameters
     * @throws AutomationCompositionRuntimeException on errors reading the automation composition parameters
     */
    public static AcRuntimeParameterGroup geParameterGroup(final String dbName) {
        try {
            return CODER.convert(getParameterGroupAsString(dbName), AcRuntimeParameterGroup.class);

        } catch (CoderException e) {
            throw new AutomationCompositionRuntimeException(Status.NOT_ACCEPTABLE,
                    "cannot read automation composition parameters", e);
        }
    }

    /**
     * Gets the standard automation composition parameters, as a String.
     *
     * @param dbName the database name
     * @return the standard automation composition parameters as string
     */
    public static String getParameterGroupAsString(final String dbName) {
        return ResourceUtils.getResourceAsString("src/test/resources/parameters/TestParameters.json")
                .replace("${dbName}", "jdbc:h2:mem:" + dbName);
    }

    /**
     * Create a List of Participants.
     *
     * @return a List of Participants
     */
    public static List<Participant> createParticipants() {
        var participant1 = createParticipant(UUID.fromString("101c62b3-8918-41b9-a747-d21eb79c6c02"));
        var participant2 = createParticipant(UUID.fromString("101c62b3-8918-41b9-a747-d21eb79c6c01"));
        var participant3 = createParticipant(UUID.fromString("101c62b3-8918-41b9-a747-d21eb79c6c03"));
        return List.of(participant1, participant2, participant3);
    }

    /**
     * Create a new Participant.
     *
     * @param participantId the participant id
     * @return a new Participant
     */
    public static Participant createParticipant(UUID participantId) {
        var participant = new Participant();
        participant.setParticipantId(participantId);
        participant.setParticipantState(ParticipantState.ON_LINE);
        return participant;
    }

    /**
     * Create a new ParticipantSupportedElementType.
     *
     * @return a new ParticipantSupportedElementType
     */
    public static ParticipantSupportedElementType createParticipantSupportedElementType() {
        var supportedElementType = new ParticipantSupportedElementType();
        supportedElementType.setTypeName("Type");
        supportedElementType.setTypeVersion("1.0.0");
        return supportedElementType;
    }

    public static UUID getParticipantId() {
        return UUID.fromString("101c62b3-8918-41b9-a747-d21eb79c6c03");
    }

    /**
     * Create a new AutomationCompositionDefinition.
     *
     * @param serviceTemplate the serviceTemplate
     * @param state the AcTypeState
     * @return a new AutomationCompositionDefinition
     */
    public static AutomationCompositionDefinition createAcDefinition(ToscaServiceTemplate serviceTemplate,
            AcTypeState state) {
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setCompositionId(UUID.randomUUID());
        acDefinition.setState(state);
        acDefinition.setServiceTemplate(serviceTemplate);
        var acElements = AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate);
        acDefinition.setElementStateMap(AcmUtils.createElementStateMap(acElements, state));
        if (AcTypeState.PRIMED.equals(state)) {
            for (var element : acDefinition.getElementStateMap().values()) {
                element.setParticipantId(getParticipantId());
            }
        }
        return acDefinition;
    }

}

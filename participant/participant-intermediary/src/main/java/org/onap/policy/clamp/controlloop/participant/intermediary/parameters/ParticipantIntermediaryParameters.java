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

package org.onap.policy.clamp.controlloop.participant.intermediary.parameters;

import lombok.Getter;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.parameters.ParameterGroupImpl;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to hold all parameters needed for participant component.
 */
@NotNull
@NotBlank
@Getter
public class ParticipantIntermediaryParameters extends ParameterGroupImpl {
    // The ID and description of this participant
    private ToscaConceptIdentifier participantId;
    private String description;

    // The participant type of this participant
    private ToscaConceptIdentifier participantType;

    // The time interval for periodic reporting of status to the CLAMP control loop server
    private long reportingTimeInterval;

    // DMaaP topics for communicating with the CLAMP control loop server
    private TopicParameterGroup clampControlLoopTopics;

    /**
     * Create the participant parameter group.
     *
     * @param instanceId instance id of the event.
     */
    public ParticipantIntermediaryParameters(final String instanceId) {
        super(instanceId);
    }
}

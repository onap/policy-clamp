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

package org.onap.policy.clamp.acm.participant.intermediary.parameters;

import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.parameters.validation.ParameterGroupConstraint;

/**
 * Class to hold all parameters needed for participant component.
 */
@Getter
@Setter
public class ParticipantIntermediaryParameters {

    // The ID and description of this participant
    @NotNull
    @Valid
    private UUID participantId;

    @NotBlank
    private String description;

    // The time interval for periodic reporting of status to the CLAMP ACM server
    @Valid
    @Positive
    private long reportingTimeIntervalMs;

    @NotNull
    @ParameterGroupConstraint
    private TopicParameterGroup clampAutomationCompositionTopics;

    @NotNull
    @Valid
    private List<ParticipantSupportedElementType> participantSupportedElementTypes;

}

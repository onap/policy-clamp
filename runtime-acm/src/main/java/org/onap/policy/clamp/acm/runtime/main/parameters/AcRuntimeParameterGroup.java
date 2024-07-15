/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021,2023-2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.main.parameters;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.parameters.validation.ParameterGroupConstraint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Class to hold all parameters needed for the ACM runtime component.
 *
 */
@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "runtime")
public class AcRuntimeParameterGroup {

    @Valid
    @NotNull
    private ParticipantParameters participantParameters;

    @NotNull
    @ParameterGroupConstraint
    private TopicParameterGroup topicParameterGroup;

    @Valid
    @NotNull
    private AcmParameters acmParameters = new AcmParameters();

    @Valid
    @NotNull
    private Topics topics = new Topics();
}

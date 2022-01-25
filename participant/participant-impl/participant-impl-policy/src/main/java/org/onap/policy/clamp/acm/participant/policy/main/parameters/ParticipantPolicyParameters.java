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

package org.onap.policy.clamp.acm.participant.policy.main.parameters;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.common.endpoints.parameters.RestClientParameters;
import org.onap.policy.common.parameters.validation.ParameterGroupConstraint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Class to hold all parameters needed for the policy participant.
 *
 */
@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "participant")
public class ParticipantPolicyParameters implements ParticipantParameters {

    @NotNull
    @Valid
    private ParticipantIntermediaryParameters intermediaryParameters;

    @NotNull
    @ParameterGroupConstraint
    private RestClientParameters policyApiParameters;

    @NotNull
    @ParameterGroupConstraint
    private RestClientParameters policyPapParameters;

    @NotBlank
    private String pdpGroup;

    @NotBlank
    private String pdpType;
}

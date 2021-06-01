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

package org.onap.policy.clamp.controlloop.participant.dcae.main.parameters;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.common.endpoints.event.comm.bus.internal.BusTopicParams;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ParameterGroupImpl;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;

/**
 * Class to hold all parameters needed for the participant dcae.
 *
 */
@NotNull
@NotBlank
@Getter
public class ParticipantDcaeParameters extends ParameterGroupImpl {

    private static final String MSG_IS_BLANK = "is blank";

    @Valid
    private BusTopicParams clampClientParameters;

    @Valid
    private BusTopicParams consulClientParameters;

    private ParticipantIntermediaryParameters intermediaryParameters;
    private PolicyModelsProviderParameters databaseProviderParameters;

    /**
     * Create the participant dcae parameter group.
     *
     * @param name the parameter group name
     */
    public ParticipantDcaeParameters(final String name) {
        super(name);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public BeanValidationResult validate() {
        BeanValidationResult result = super.validate();
        if (result.isValid()) {
            result.addResult(checkMissingMandatoryParams(clampClientParameters));
            result.addResult(checkMissingMandatoryParams(consulClientParameters));
        }
        return result;
    }

    private BeanValidationResult checkMissingMandatoryParams(BusTopicParams clientParameters) {
        BeanValidationResult result = new BeanValidationResult(clientParameters.getClientName(), clientParameters);
        if (clientParameters.isHostnameInvalid()) {
            result.addResult("Host", clientParameters.getHostname(), ValidationStatus.INVALID, MSG_IS_BLANK);
        }
        if (clientParameters.isClientNameInvalid()) {
            result.addResult("Name", clientParameters.getClientName(), ValidationStatus.INVALID, MSG_IS_BLANK);
        }
        if (StringUtils.isBlank(clientParameters.getPassword())) {
            result.addResult("Password", clientParameters.getPassword(), ValidationStatus.INVALID, MSG_IS_BLANK);
        }
        if (StringUtils.isBlank(clientParameters.getUserName())) {
            result.addResult("UserName", clientParameters.getUserName(), ValidationStatus.INVALID, MSG_IS_BLANK);
        }
        if (clientParameters.isPortInvalid()) {
            result.addResult("Port", clientParameters.getPort(), ValidationStatus.INVALID, "is not valid");
        }
        return result;
    }
}

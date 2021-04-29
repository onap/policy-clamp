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
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
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
    @Valid
    private RestServerParameters clampClientParameters;

    @Valid
    private RestServerParameters consulClientParameters;

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
            StringBuilder errorMsg = new StringBuilder();
            StringBuilder missingSourceParams = checkMissingMandatoryParams(clampClientParameters);
            if (missingSourceParams.length() > 0) {
                errorMsg.append(missingSourceParams.append("missing in clamp client parameters. "));
            }
            StringBuilder missingSinkParams = checkMissingMandatoryParams(consulClientParameters);
            if (missingSinkParams.length() > 0) {
                errorMsg.append(missingSinkParams.append("missing in consul client parameters."));
            }

            if (errorMsg.length() > 0) {
                errorMsg.insert(0, "Mandatory parameters are missing. ");
                result.setResult(ValidationStatus.INVALID, errorMsg.toString());
            }
        }
        return result;
    }

    private StringBuilder checkMissingMandatoryParams(RestServerParameters clientParameters) {
        StringBuilder missingParams = new StringBuilder();
        if (StringUtils.isBlank(clientParameters.getHost())) {
            missingParams.append("Host, ");
        }
        if (StringUtils.isBlank(clientParameters.getName())) {
            missingParams.append("Name, ");
        }
        if (StringUtils.isBlank(clientParameters.getPassword())) {
            missingParams.append("Password, ");
        }
        if (StringUtils.isBlank(clientParameters.getUserName())) {
            missingParams.append("UserName, ");
        }
        if (clientParameters.getPort() <= 0 || clientParameters.getPort() >= 65535) {
            missingParams.append("Port, ");
        }
        return missingParams;
    }
}

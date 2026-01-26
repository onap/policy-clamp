/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2026 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.parameters.topic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ParameterGroupImpl;
import org.onap.policy.common.parameters.ValidationStatus;

/**
 * Class to hold all parameters needed for topic properties.
 *
 * @author Ajith Sreekumar (ajith.sreekumar@est.tech)
 */
@Getter
@Setter
public class TopicParameterGroup extends ParameterGroupImpl {

    @NotNull
    private List<@Valid TopicParameters> topicSources;
    @NotNull
    private List<@Valid TopicParameters> topicSinks;

    public TopicParameterGroup() {
        super(TopicParameterGroup.class.getSimpleName());
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public BeanValidationResult validate() {
        BeanValidationResult result = super.validate();
        if (result.isValid()) {
            var errorMsg = new StringBuilder();
            StringBuilder missingSourceParams = checkMissingMandatoryParams(topicSources);
            if (!missingSourceParams.isEmpty()) {
                errorMsg.append(missingSourceParams.append("missing in topicSources. "));
            }
            StringBuilder missingSinkParams = checkMissingMandatoryParams(topicSinks);
            if (!missingSinkParams.isEmpty()) {
                errorMsg.append(missingSinkParams.append("missing in topicSinks."));
            }

            if (!errorMsg.isEmpty()) {
                errorMsg.insert(0, "Mandatory parameters are missing. ");
                result.setResult(ValidationStatus.INVALID, errorMsg.toString());
            }
        }
        return result;
    }

    private StringBuilder checkMissingMandatoryParams(List<TopicParameters> topicParametersList) {
        var missingParams = new StringBuilder();
        for (TopicParameters topicParameters : topicParametersList) {
            if (StringUtils.isBlank(topicParameters.getTopic())) {
                missingParams.append("topic, ");
            }
            if (StringUtils.isBlank(topicParameters.getTopicCommInfrastructure())) {
                missingParams.append("topicCommInfrastructure, ");
            }
            if (null == topicParameters.getServers() || topicParameters.getServers().isEmpty()) {
                missingParams.append("servers, ");
            }
        }
        return missingParams;
    }
}

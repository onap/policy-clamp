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

package org.onap.policy.clamp.controlloop.participant.kubernetes.parameters;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.common.parameters.ParameterGroupImpl;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;

/**
 * Class to hold all parameters needed for the kubernetes participant.
 *
 */
@NotNull
@NotBlank
@Getter
public class ParticipantK8sParameters extends ParameterGroupImpl {
    public static final String DEFAULT_LOCAL_CHART_DIR = "/var/helm-manager/local-charts";
    public static final String DEFAULT_INFO_FILE_NAME = "CHART_INFO.json";

    @Valid
    private ParticipantIntermediaryParameters intermediaryParameters;
    @Valid
    private PolicyModelsProviderParameters databaseProviderParameters;


    private String localChartDirectory = DEFAULT_LOCAL_CHART_DIR;
    private String infoFileName = DEFAULT_INFO_FILE_NAME;

    /**
     * Create the kubernetes participant parameter group.
     *
     * @param name the parameter group name
     */
    public ParticipantK8sParameters(final String name) {
        super(name);
    }
}

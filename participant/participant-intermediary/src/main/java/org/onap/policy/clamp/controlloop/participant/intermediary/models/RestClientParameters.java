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

package org.onap.policy.clamp.controlloop.participant.intermediary.models;

import org.apache.commons.lang3.StringUtils;
import org.onap.policy.common.endpoints.event.comm.bus.internal.BusTopicParams;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ParameterGroup;
import org.onap.policy.common.parameters.ValidationStatus;

public class RestClientParameters extends BusTopicParams implements ParameterGroup {

    private static final String MSG_IS_BLANK = "is blank";

    @Override
    public String getName() {
        return getClientName();
    }

    @Override
    public void setName(String name) {
        setClientName(name);
    }

    @Override
    public BeanValidationResult validate() {
        BeanValidationResult result = new BeanValidationResult(getClientName(), this);
        if (isHostnameInvalid()) {
            result.addResult("Host", getHostname(), ValidationStatus.INVALID, MSG_IS_BLANK);
        }
        if (isClientNameInvalid()) {
            result.addResult("Name", getClientName(), ValidationStatus.INVALID, MSG_IS_BLANK);
        }
        if (StringUtils.isBlank(getPassword())) {
            result.addResult("Password", getPassword(), ValidationStatus.INVALID, MSG_IS_BLANK);
        }
        if (StringUtils.isBlank(getUserName())) {
            result.addResult("UserName", getUserName(), ValidationStatus.INVALID, MSG_IS_BLANK);
        }
        if (isPortInvalid()) {
            result.addResult("Port", getPort(), ValidationStatus.INVALID, "is not valid");
        }
        return result;
    }
}
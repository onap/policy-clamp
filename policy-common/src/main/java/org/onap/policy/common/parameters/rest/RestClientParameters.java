/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021, 2024 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.parameters.rest;

import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ParameterGroup;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.common.parameters.topic.BusTopicParams;

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
        var result = new BeanValidationResult(getClientName(), this);
        if (isHostnameInvalid()) {
            result.addResult("hostname", getHostname(), ValidationStatus.INVALID, MSG_IS_BLANK);
        }
        if (isClientNameInvalid()) {
            result.addResult("clientName", getClientName(), ValidationStatus.INVALID, MSG_IS_BLANK);
        }
        if (isPortInvalid()) {
            result.addResult("port", getPort(), ValidationStatus.INVALID, "is not valid");
        }
        return result;
    }
}

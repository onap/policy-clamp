/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022,2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.element.main.parameters;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.onap.policy.clamp.models.acm.messages.rest.element.KafkaConfig;
import org.onap.policy.common.endpoints.parameters.TopicParameters;

@Data
@EqualsAndHashCode(callSuper = false)
public class ElementTopicParameters extends TopicParameters {

    /**
     * Constructor.
     * @param parameters KafkaConfig
     */
    public ElementTopicParameters(KafkaConfig parameters) {
        super();
        this.setTopic(parameters.getListenerTopic());
        this.setServers(List.of(parameters.getServer()));
        this.setFetchTimeout(parameters.getFetchTimeout());
        this.setTopicCommInfrastructure(parameters.getTopicCommInfrastructure());
        this.setUseHttps(parameters.isUseHttps());
    }

}

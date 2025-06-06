/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.api;

import java.util.Map;
import java.util.UUID;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record InstanceElementDto(UUID instanceId, UUID elementId,
            Map<String, Object> inProperties, Map<String, Object> outProperties, ElementState state) {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceElementDto.class);

    /**
     * Constructor for backward compatibility.
     *
     * @param instanceId AC instanceid
     * @param elementId AC element instanceid
     * @param toscaServiceTemplateFragment backward compatibility
     * @param inProperties AC element instance In properties
     * @param outProperties AC element instance Out properties
     */
    public InstanceElementDto(UUID instanceId, UUID elementId, ToscaServiceTemplate toscaServiceTemplateFragment,
            Map<String, Object> inProperties, Map<String, Object> outProperties) {
        this(instanceId, elementId, inProperties, outProperties, ElementState.PRESENT);
        LOGGER.debug("Obsolete InstanceElementDto constructor: toscaServiceTemplateFragment field {} is not used!",
                toscaServiceTemplateFragment);
    }

    public InstanceElementDto(UUID instanceId, UUID elementId,
            Map<String, Object> inProperties, Map<String, Object> outProperties) {
        this(instanceId, elementId, inProperties, outProperties, ElementState.PRESENT);
    }
}

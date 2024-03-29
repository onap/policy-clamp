/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.concepts;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.onap.policy.models.base.PfUtils;

/**
 * Class to represent a automation composition element info instance.
 */
@NoArgsConstructor
@Data
@ToString
public class AutomationCompositionElementInfo {

    private UUID automationCompositionElementId;

    private DeployState deployState = DeployState.UNDEPLOYED;

    private LockState lockState = LockState.LOCKED;

    private String operationalState;

    private String useState;

    private Map<String, Object> outProperties = new LinkedHashMap<>();

    /**
     * Copy constructor, does a deep copy but as all fields here are immutable, it's just a regular copy.
     *
     * @param otherElement the other element to copy from
     */
    public AutomationCompositionElementInfo(AutomationCompositionElementInfo otherElement) {
        this.automationCompositionElementId = otherElement.automationCompositionElementId;
        this.deployState = otherElement.deployState;
        this.lockState = otherElement.lockState;
        this.operationalState = otherElement.operationalState;
        this.useState = otherElement.useState;
        this.outProperties = PfUtils.mapMap(otherElement.outProperties, UnaryOperator.identity());
    }
}

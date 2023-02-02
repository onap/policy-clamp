/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2023 Nordix Foundation.
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

import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.collections4.MapUtils;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaEntity;

/**
 * Class to represent an automation composition instance.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class AutomationComposition extends ToscaEntity implements Comparable<AutomationComposition> {

    private UUID instanceId;

    @NonNull
    private UUID compositionId;

    @NonNull
    private AutomationCompositionState state = AutomationCompositionState.UNINITIALISED;

    @NonNull
    private AutomationCompositionOrderedState orderedState = AutomationCompositionOrderedState.UNINITIALISED;

    @NonNull
    private DeployState deployState = DeployState.UNDEPLOYED;

    @NonNull
    private LockState lockState = LockState.NONE;

    private Map<UUID, AutomationCompositionElement> elements;

    /**
     * Copy contructor, does a deep copy.
     *
     * @param otherAutomationComposition the other element to copy from
     */
    public AutomationComposition(final AutomationComposition otherAutomationComposition) {
        super(otherAutomationComposition);
        this.instanceId = otherAutomationComposition.instanceId;
        this.compositionId = otherAutomationComposition.compositionId;
        this.state = otherAutomationComposition.state;
        this.orderedState = otherAutomationComposition.orderedState;
        this.deployState = otherAutomationComposition.deployState;
        this.lockState = otherAutomationComposition.lockState;
        this.elements = PfUtils.mapMap(otherAutomationComposition.elements, AutomationCompositionElement::new);
    }

    @Override
    public int compareTo(final AutomationComposition other) {
        return compareNameVersion(this, other);
    }

    /**
     * Set the ordered state on the automation composition and on all its automation composition elements.
     *
     * @param orderedState the state we want the automation composition to transition to
     */
    public void setCascadedOrderedState(final AutomationCompositionOrderedState orderedState) {
        this.orderedState = orderedState;

        if (MapUtils.isEmpty(elements)) {
            return;
        }

        elements.values().forEach(element -> element.setOrderedState(orderedState));
    }
}

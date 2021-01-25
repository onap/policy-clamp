/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.controlloop.concepts;

import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.onap.policy.models.base.PfNameVersion;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent a control loop instance.
 */
@NoArgsConstructor
@Data
@ToString
public class ControlLoop implements PfNameVersion {
    @NonNull
    private String name;

    @NonNull
    private String version;

    @NonNull
    private ToscaConceptIdentifier definition;

    @NonNull
    private ControlLoopState state;

    @NonNull
    private ControlLoopOrderedState orderedState;

    private String description;
    private List<ControlLoopElement> elements;

    public ToscaConceptIdentifier getId() {
        return new ToscaConceptIdentifier(name, version);
    }

    public String getDefinitionName() {
        return definition.getName();
    }

    public String getDefinitionVersion() {
        return definition.getVersion();
    }

    /**
     * Copy contructor, does a deep copy.
     *
     * @param otherControlLoop the other element to copy from
     */
    public ControlLoop(final ControlLoop otherControlLoop) {
        this.name = otherControlLoop.name;
        this.version = otherControlLoop.version;
        this.definition = otherControlLoop.definition;
        this.state = otherControlLoop.state;
        this.orderedState = otherControlLoop.orderedState;
        this.description = otherControlLoop.description;
        this.elements = PfUtils.mapList(otherControlLoop.elements, ControlLoopElement::new);
    }

    /**
     * Set the ordered state on the control loop and on all its control loop elements.
     *
     * @param orderedState the state we want the control loop to transition to
     */
    public void setCascadedOrderedState(final ControlLoopOrderedState orderedState) {
        this.orderedState = orderedState;

        if (CollectionUtils.isEmpty(elements)) {
            return;
        }

        elements.forEach(element -> element.setOrderedState(orderedState));
    }

    /**
     * Find the element with a given UUID for the control loop.
     *
     * @param id the UUID to search for
     * @return the element or null if its not found
     */

    public ControlLoopElement getElement(final UUID id) {
        if (CollectionUtils.isEmpty(elements)) {
            return null;
        }

        for (ControlLoopElement element : elements) {
            if (id.equals(element.getId())) {
                return element;
            }
        }

        return null;
    }
}

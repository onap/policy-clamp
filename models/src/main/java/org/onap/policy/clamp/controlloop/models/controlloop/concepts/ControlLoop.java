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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.collections4.MapUtils;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaEntity;

/**
 * Class to represent a control loop instance.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class ControlLoop extends ToscaEntity implements Comparable<ControlLoop> {
    @NonNull
    private ToscaConceptIdentifier definition = new ToscaConceptIdentifier(PfConceptKey.getNullKey());

    @NonNull
    private ControlLoopState state = ControlLoopState.UNINITIALISED;

    @NonNull
    private ControlLoopOrderedState orderedState = ControlLoopOrderedState.UNINITIALISED;

    private Map<UUID, ControlLoopElement> elements;

    @NonNull
    private Boolean primed = false;

    @Override
    public String getType() {
        return definition.getName();
    }

    @Override
    public String getTypeVersion() {
        return definition.getVersion();
    }

    /**
     * Copy contructor, does a deep copy.
     *
     * @param otherControlLoop the other element to copy from
     */
    public ControlLoop(final ControlLoop otherControlLoop) {
        super(otherControlLoop);
        this.definition = new ToscaConceptIdentifier(otherControlLoop.definition);
        this.state = otherControlLoop.state;
        this.orderedState = otherControlLoop.orderedState;
        this.elements = PfUtils.mapMap(otherControlLoop.elements, ControlLoopElement::new);
        this.primed = otherControlLoop.primed;
    }

    @Override
    public int compareTo(final ControlLoop other) {
        return compareNameVersion(this, other);
    }

    /**
     * Set the ordered state on the control loop and on all its control loop elements.
     *
     * @param orderedState the state we want the control loop to transition to
     */
    public void setCascadedOrderedState(final ControlLoopOrderedState orderedState) {
        this.orderedState = orderedState;

        if (MapUtils.isEmpty(elements)) {
            return;
        }

        elements.values().forEach(element -> element.setOrderedState(orderedState));
    }

    /**
     * Get a list of control loop element statistics.
     *
     * @param controlLoop the control loop
     * @return List of ClElementStatistics
     */
    public List<ClElementStatistics> getControlLoopElementStatisticsList(final ControlLoop controlLoop) {
        if (MapUtils.isEmpty(controlLoop.elements)) {
            return List.of();
        }

        return controlLoop.elements.values().stream().map(ControlLoopElement::getClElementStatistics)
                .collect(Collectors.toList());
    }
}

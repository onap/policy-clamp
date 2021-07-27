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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent a control loop instance.
 */
@NoArgsConstructor
@Data
@ToString
public class ControlLoopElement {
    @NonNull
    private UUID id = UUID.randomUUID();

    @NonNull
    private ToscaConceptIdentifier definition = new ToscaConceptIdentifier(PfConceptKey.getNullKey());

    @NonNull
    private ToscaConceptIdentifier participantType = new ToscaConceptIdentifier(PfConceptKey.getNullKey());

    @NonNull
    private ToscaConceptIdentifier participantId = new ToscaConceptIdentifier(PfConceptKey.getNullKey());

    @NonNull
    private ControlLoopState state = ControlLoopState.UNINITIALISED;

    @NonNull
    private ControlLoopOrderedState orderedState = ControlLoopOrderedState.UNINITIALISED;

    private String description;

    private ClElementStatistics clElementStatistics;

    // A map indexed by the property name. Each map entry is the serialized value of the property,
    // which can be deserialized into an instance of the type of the property.
    private Map<String, String> commonPropertiesMap = new LinkedHashMap<>();

    /**
     * Copy constructor, does a deep copy but as all fields here are immutable, it's just a regular copy.
     *
     * @param otherElement the other element to copy from
     */
    public ControlLoopElement(final ControlLoopElement otherElement) {
        this.id = otherElement.id;
        this.definition = new ToscaConceptIdentifier(otherElement.definition);
        this.participantType = new ToscaConceptIdentifier(otherElement.participantType);
        this.participantId = new ToscaConceptIdentifier(otherElement.participantId);
        this.state = otherElement.state;
        this.orderedState = otherElement.orderedState;
        this.description = otherElement.description;
        this.clElementStatistics = otherElement.clElementStatistics;
        this.commonPropertiesMap = PfUtils.mapMap(otherElement.commonPropertiesMap, UnaryOperator.identity());
    }
}

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

import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent a control loop instance.
 */
@NoArgsConstructor
@Data
@ToString
public class ControlLoopElement {
    @NonNull
    private UUID id;

    @NonNull
    private ToscaConceptIdentifier definition;

    @NonNull
    private ToscaConceptIdentifier participantId;

    @NonNull
    private ControlLoopState state;

    private ControlLoopOrderedState orderedState;

    private String description;

    /**
     * Copy contructor, does a deep copy but as all fields here are immutable, it's just a regular copy.
     *
     * @param otherElement the other element to copy from
     */
    public ControlLoopElement(final ControlLoopElement otherElement) {
        this.id = otherElement.id;
        this.definition = otherElement.definition;
        this.participantId = otherElement.participantId;
        this.state = otherElement.state;
        this.orderedState = otherElement.orderedState;
        this.description = otherElement.description;
    }
}

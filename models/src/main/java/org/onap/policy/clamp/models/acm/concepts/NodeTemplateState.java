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

import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

@NoArgsConstructor
@Data
@EqualsAndHashCode
public class NodeTemplateState {

    private UUID nodeTemplateStateId;

    // participantId assigned to this element
    private UUID participantId;

    private ToscaConceptIdentifier nodeTemplateId;

    private AcTypeState state;

    /**
     * Copy constructor, does a deep copy but as all fields here are immutable, it's just a regular copy.
     *
     * @param copyConstructor the NodeTemplateState to copy from
     */
    public NodeTemplateState(NodeTemplateState copyConstructor) {
        this.nodeTemplateStateId = copyConstructor.nodeTemplateStateId;
        this.participantId = copyConstructor.participantId;
        this.nodeTemplateId = new ToscaConceptIdentifier(copyConstructor.nodeTemplateId);
        this.state = copyConstructor.state;
    }
}

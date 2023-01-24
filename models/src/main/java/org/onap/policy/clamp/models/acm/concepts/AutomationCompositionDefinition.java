/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation.
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

@NoArgsConstructor
@Data
@EqualsAndHashCode
public class AutomationCompositionDefinition {

    @NonNull
    private UUID compositionId;

    @NonNull
    private ToscaServiceTemplate serviceTemplate;

    @NonNull
    private AcTypeState state;

    @NonNull
    // Map used to store prime state with key as NodeTemplate Name and value as NodeTemplateState
    private Map<String, NodeTemplateState> elementStateMap = new HashMap<>();

    /**
     * Copy contructor, does a deep copy.
     *
     * @param otherAcmDefinition the other element to copy from
     */
    public AutomationCompositionDefinition(final AutomationCompositionDefinition otherAcmDefinition) {
        this.compositionId = otherAcmDefinition.compositionId;
        this.serviceTemplate = new ToscaServiceTemplate(otherAcmDefinition.serviceTemplate);
        this.state = otherAcmDefinition.state;
        this.elementStateMap = PfUtils.mapMap(otherAcmDefinition.elementStateMap, NodeTemplateState::new);
    }
}

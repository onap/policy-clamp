/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.onap.policy.models.base.PfUtils;

@NoArgsConstructor
@Data
public class AutomationCompositionRollback {

    @NonNull
    private UUID instanceId;

    @NonNull
    private UUID compositionId;

    @NonNull
    private Map<UUID, AutomationCompositionElement> elements = new LinkedHashMap<>();

    /**
     * Copy constructor, does a deep copy.
     *
     * @param otherAcmRollback the other element to copy from
     */
    public AutomationCompositionRollback(final AutomationCompositionRollback otherAcmRollback) {
        this.instanceId = otherAcmRollback.instanceId;
        this.compositionId = otherAcmRollback.compositionId;
        this.elements = PfUtils.mapMap(otherAcmRollback.elements, AutomationCompositionElement::new);
    }

    /**
     * Create a copy from an automation composition.
     *
     * @param automationComposition the composition being migrated that needs a copy
     */
    public AutomationCompositionRollback(final AutomationComposition automationComposition) {
        this.instanceId = automationComposition.getInstanceId();
        this.compositionId = automationComposition.getCompositionId();
        this.elements = PfUtils.mapMap(automationComposition.getElements(), AutomationCompositionElement::new);
    }
}

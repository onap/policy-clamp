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

package org.onap.policy.clamp.models.acm.concepts;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent a participant definition update instance.
 */
@Getter
@NoArgsConstructor
@Data
@ToString
public class ParticipantDefinition {

    private ToscaConceptIdentifier participantId;

    private ToscaConceptIdentifier participantType;

    // List of AutomationCompositionElementDefinition values for a particular participant
    private List<AutomationCompositionElementDefinition> automationCompositionElementDefinitionList = new ArrayList<>();

    /**
     * Copy constructor, does a deep copy but as all fields here are immutable, it's just a regular copy.
     *
     * @param participantDefinition the participant definition to copy from
     */
    public ParticipantDefinition(final ParticipantDefinition participantDefinition) {
        this.participantId = participantDefinition.participantId;
        this.participantType = participantDefinition.participantType;
        this.automationCompositionElementDefinitionList =
            PfUtils.mapList(participantDefinition.automationCompositionElementDefinitionList,
                AutomationCompositionElementDefinition::new);
    }
}

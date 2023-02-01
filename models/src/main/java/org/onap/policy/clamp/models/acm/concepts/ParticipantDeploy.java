/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
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
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.onap.policy.models.base.PfUtils;

/**
 * Class to represent a participant definition instance for Deploy.
 */
@Getter
@NoArgsConstructor
@Data
@ToString
public class ParticipantDeploy {

    private UUID participantId;

    // List of AutomationCompositionElement values for a particular participant
    private List<AutomationCompositionElement> automationCompositionElementList = new ArrayList<>();

    // List of Automation Composition Element Deploy for a particular participant
    private List<AcElementDeploy> acElementList = new ArrayList<>();

    /**
     * Copy constructor, does a deep copy but as all fields here are immutable, it's just a regular copy.
     *
     * @param copyConstructor the participant with updates to copy from
     */
    public ParticipantDeploy(final ParticipantDeploy copyConstructor) {
        this.participantId = copyConstructor.participantId;
        this.automationCompositionElementList = PfUtils.mapList(
            copyConstructor.automationCompositionElementList, AutomationCompositionElement::new);
        this.acElementList = PfUtils.mapList(copyConstructor.acElementList, AcElementDeploy::new);
    }
}

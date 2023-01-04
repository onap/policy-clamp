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

package org.onap.policy.clamp.models.acm.messages.dmaap.participant;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.models.base.PfUtils;

/**
 * Class to represent the PARTICIPANT_STATUS message that all the participants send to the ACM runtime.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ParticipantStatus extends ParticipantMessage {

    // State and health status of the participant
    private ParticipantState state;

    // A list of ParticipantDefinition updates, returned in response to ParticipantStatusReq only
    private List<ParticipantDefinition> participantDefinitionUpdates = new ArrayList<>();

    // List of AutomationCompositionInfo types with AutomationCompositionId and its state
    private List<AutomationCompositionInfo> automationCompositionInfoList = new ArrayList<>();

    /**
     * Constructor for instantiating ParticipantStatus class with message name.
     *
     */
    public ParticipantStatus() {
        super(ParticipantMessageType.PARTICIPANT_STATUS);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ParticipantStatus(final ParticipantStatus source) {
        super(source);

        this.state = source.state;
        this.participantDefinitionUpdates =
            PfUtils.mapList(source.participantDefinitionUpdates, ParticipantDefinition::new);
        this.automationCompositionInfoList =
            PfUtils.mapList(source.automationCompositionInfoList, AutomationCompositionInfo::new);
    }
}

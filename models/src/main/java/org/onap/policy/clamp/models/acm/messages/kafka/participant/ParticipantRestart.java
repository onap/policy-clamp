/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.messages.kafka.participant;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantRestartAc;
import org.onap.policy.models.base.PfUtils;

@Getter
@Setter
@ToString(callSuper = true)
public class ParticipantRestart extends ParticipantMessage {

    // composition state
    AcTypeState state;

    // element definition
    private List<ParticipantDefinition> participantDefinitionUpdates = new ArrayList<>();

    // automation composition instances list
    private List<ParticipantRestartAc> automationcompositionList = new ArrayList<>();

    /**
     * Constructor.
     */
    public ParticipantRestart() {
        super(ParticipantMessageType.PARTICIPANT_RESTART);
    }

    /**
     * Constructor with message type.
     * @param messageType messageType
     */
    public ParticipantRestart(ParticipantMessageType messageType) {
        super(messageType);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ParticipantRestart(ParticipantRestart source) {
        super(source);
        this.participantDefinitionUpdates =
                PfUtils.mapList(source.participantDefinitionUpdates, ParticipantDefinition::new);
        this.automationcompositionList = PfUtils.mapList(source.automationcompositionList, ParticipantRestartAc::new);
    }
}

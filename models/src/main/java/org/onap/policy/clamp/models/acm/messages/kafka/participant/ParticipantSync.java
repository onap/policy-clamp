/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
public class ParticipantSync extends ParticipantMessage {

    // composition state
    private AcTypeState state;

    // element definition
    private List<ParticipantDefinition> participantDefinitionUpdates = new ArrayList<>();

    // automation composition instances list
    private List<ParticipantRestartAc> automationcompositionList = new ArrayList<>();

    private Set<UUID> excludeReplicas = new HashSet<>();
    private boolean restarting = false;
    private boolean delete = false;

    /**
     * Constructor.
     */
    public ParticipantSync() {
        super(ParticipantMessageType.PARTICIPANT_SYNC_MSG);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ParticipantSync(ParticipantSync source) {
        super(source);
        this.state = source.state;
        this.participantDefinitionUpdates =
                PfUtils.mapList(source.participantDefinitionUpdates, ParticipantDefinition::new);
        this.automationcompositionList = PfUtils.mapList(source.automationcompositionList, ParticipantRestartAc::new);
        this.excludeReplicas = new HashSet<>(source.excludeReplicas);
        this.restarting = source.restarting;
        this.delete = source.delete;
    }
}

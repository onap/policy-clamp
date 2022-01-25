/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.models.base.PfUtils;

/**
 * Class to represent the PARTICIPANT_UPDATE message that the ACM runtime sends to a participant.
 * the ACM Runtime sends automation composition element Definitions and Common Parameter Values to Participants.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ParticipantUpdate extends ParticipantMessage {

    // A list of updates to ParticipantDefinitions
    private List<ParticipantDefinition> participantDefinitionUpdates = new ArrayList<>();

    /**
     * Constructor for instantiating ParticipantUpdate class with message name.
     *
     */
    public ParticipantUpdate() {
        super(ParticipantMessageType.PARTICIPANT_UPDATE);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ParticipantUpdate(ParticipantUpdate source) {
        super(source);

        this.participantDefinitionUpdates = PfUtils.mapList(source.participantDefinitionUpdates,
            ParticipantDefinition::new);
    }
}

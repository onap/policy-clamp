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

package org.onap.policy.clamp.models.acm.messages.dmaap.participant;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.models.base.PfUtils;


/**
 * Class to represent the PROPERTIES_UPDATE message that the ACM runtime sends to a participant.
 * The ACM Runtime sends updated automation composition element property values to Participants.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class PropertiesUpdate extends ParticipantMessage {

    // A list of updates to AC element properties
    private List<ParticipantDeploy> participantUpdatesList = new ArrayList<>();

    /**
     * Constructor for instantiating properties update class with message name.
     *
     */
    public PropertiesUpdate() {
        super(ParticipantMessageType.PROPERTIES_UPDATE);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public PropertiesUpdate(PropertiesUpdate source) {
        super(source);
        this.participantUpdatesList = PfUtils.mapList(source.participantUpdatesList, ParticipantDeploy::new);
    }
}

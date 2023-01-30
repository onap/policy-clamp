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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeployAck;
import org.onap.policy.models.base.PfUtils;

/**
 * Class to represent the AUTOMATION_COMPOSITION_ACK message that a participant sends
 * to automation composition runtime as an acknowledgement to either AUTOMATION_COMPOSITION_UPDATE
 * or AUTOMATION_COMPOSITION_STATE_CHANGE message.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class AutomationCompositionDeployAck extends ParticipantAckMessage {

    private UUID automationCompositionId;
    private Integer startPhase;

    // A map with AutomationCompositionElementID as its key, and a pair of result and message as value per
    // AutomationCompositionElement.
    private Map<UUID, AcElementDeployAck> automationCompositionResultMap = new LinkedHashMap<>();

    /**
     * Constructor for instantiating ParticipantRegisterAck class with message name.
     *
     */
    public AutomationCompositionDeployAck(final ParticipantMessageType messageType) {
        super(messageType);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public AutomationCompositionDeployAck(final AutomationCompositionDeployAck source) {
        super(source);
        this.automationCompositionId = source.automationCompositionId;
        this.startPhase = source.startPhase;
        this.automationCompositionResultMap =
            PfUtils.mapMap(source.automationCompositionResultMap, UnaryOperator.identity());
    }
}

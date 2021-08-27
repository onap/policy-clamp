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

package org.onap.policy.clamp.controlloop.models.messages.dmaap.participant;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementAck;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent the CONTROLLOOP_ACK message that a participant sends
 * to control loop runtime as an acknowledgement to either ControlLoopUpdate
 * or ControlLoopStateChange message.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ControlLoopAck extends ParticipantAckMessage {

    private ToscaConceptIdentifier controlLoopId;

    // A map with ControlLoopElementID as its key, and a pair of result and message as value per
    // ControlLoopElement.
    private Map<UUID, ControlLoopElementAck> controlLoopResultMap = new LinkedHashMap<>();

    /**
     * Constructor for instantiating ParticipantRegisterAck class with message name.
     *
     */
    public ControlLoopAck(final ParticipantMessageType messageType) {
        super(messageType);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ControlLoopAck(final ControlLoopAck source) {
        super(source);
        this.controlLoopId = source.controlLoopId;
        this.controlLoopResultMap = PfUtils.mapMap(source.controlLoopResultMap, UnaryOperator.identity());
    }
}

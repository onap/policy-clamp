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

package org.onap.policy.clamp.models.acm.messages.dmaap.participant;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;

/**
 * Class to represent the AUTOMATION_COMPOSITION_STATE_CHANGE message that the automation composition runtime will send
 * to participants to change the state of an automation composition they are running.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class AutomationCompositionStateChange extends ParticipantMessage {
    private AutomationCompositionOrderedState orderedState;
    private AutomationCompositionState currentState;
    private Integer startPhase;

    /**
     * Constructor for instantiating class with message name.
     *
     */
    public AutomationCompositionStateChange() {
        super(ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public AutomationCompositionStateChange(AutomationCompositionStateChange source) {
        super(source);

        this.orderedState = source.orderedState;
        this.currentState = source.currentState;
    }
}

/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021,2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.supervision.comm;

import org.onap.policy.clamp.acm.runtime.config.messaging.Listener;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler;
import org.onap.policy.clamp.common.acm.utils.NetLoggerUtil;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.springframework.stereotype.Component;

/**
 * Listener for AutomationCompositionUpdateAck messages sent by participants.
 */
@Component
public class AutomationCompositionUpdateAckListener extends ScoListener<AutomationCompositionDeployAck>
    implements Listener<AutomationCompositionDeployAck> {

    private final SupervisionAcHandler supervisionHandler;

    /**
     * Constructs the object.
     */
    public AutomationCompositionUpdateAckListener(SupervisionAcHandler supervisionHandler) {
        super(AutomationCompositionDeployAck.class);
        this.supervisionHandler = supervisionHandler;
    }

    @Override
    public void onTopicEvent(final CommInfrastructure infra, final String topic, final StandardCoderObject sco,
        final AutomationCompositionDeployAck automationCompositionUpdateAckMessage) {
        NetLoggerUtil.log(NetLoggerUtil.EventType.IN, infra, topic, automationCompositionUpdateAckMessage.toString());
        supervisionHandler.handleAutomationCompositionUpdateAckMessage(automationCompositionUpdateAckMessage);
    }

    @Override
    public ScoListener<AutomationCompositionDeployAck> getScoListener() {
        return this;
    }

    @Override
    public String getType() {
        return ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY_ACK.name();
    }
}

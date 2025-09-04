/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionParticipantHandler;
import org.onap.policy.clamp.common.acm.utils.NetLoggerUtil;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantReqSync;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.message.bus.event.Topic;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.springframework.stereotype.Component;

@Component
public class ParticipantReqSyncListener extends ScoListener<ParticipantReqSync>
        implements Listener<ParticipantReqSync> {

    private final SupervisionParticipantHandler supervisionParticipantHandler;

    public ParticipantReqSyncListener(SupervisionParticipantHandler supervisionParticipantHandler) {
        super(ParticipantReqSync.class);
        this.supervisionParticipantHandler = supervisionParticipantHandler;
    }

    @Override
    public String getType() {
        return ParticipantMessageType.PARTICIPANT_REQ_SYNC_MSG.name();
    }

    @Override
    public ScoListener<ParticipantReqSync> getScoListener() {
        return this;
    }

    @Override
    public void onTopicEvent(final Topic.CommInfrastructure infra, final String topic, final StandardCoderObject sco,
            ParticipantReqSync participantReqSync) {
        NetLoggerUtil.log(NetLoggerUtil.EventType.IN, infra, topic, participantReqSync.toString());
        supervisionParticipantHandler.handleParticipantReqSync(participantReqSync);
    }
}

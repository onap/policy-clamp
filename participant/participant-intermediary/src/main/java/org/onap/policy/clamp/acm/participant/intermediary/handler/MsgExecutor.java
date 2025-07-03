/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import io.opentelemetry.context.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.AutomationCompositionMsg;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantReqSync;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MsgExecutor {

    private final ExecutorService executor = Context.taskWrapping(Executors.newSingleThreadExecutor());

    private final CacheProvider cacheProvider;
    private final ParticipantMessagePublisher publisher;

    /**
     * Execute the message if all data are present or put on Hold if something is missing.
     *
     * @param message the message
     */
    public void execute(AutomationCompositionMsg<?> message) {
        if (validExecution(message)) {
            message.execute();
        } else {
            cacheProvider.getMessagesOnHold().put(message.getKey(), message);
            var participantReqSync = new ParticipantReqSync();
            participantReqSync.setParticipantId(cacheProvider.getParticipantId());
            participantReqSync.setReplicaId(cacheProvider.getReplicaId());
            participantReqSync.setCompositionId(message.getCompositionId());
            participantReqSync.setAutomationCompositionId(message.getInstanceId());
            participantReqSync.setCompositionTargetId(message.getCompositionTargetId());
            publisher.sendParticipantReqSync(participantReqSync);
        }
    }

    /**
     * Check if messages on hold can be executed.
     */
    public void check() {
        executor.submit(this::checkAndExecute);
    }

    private void checkAndExecute() {
        var executable = cacheProvider.getMessagesOnHold().values().stream()
                .filter(this::validExecution).toList();
        executable.forEach(AutomationCompositionMsg::execute);
        executable.forEach(msg -> cacheProvider.getMessagesOnHold().remove(msg.getKey()));
    }

    private boolean validExecution(AutomationCompositionMsg<?> message) {
        var result = true;
        if (message.getCompositionId() != null) {
            var valid = cacheProvider.isCompositionDefinitionUpdated(message.getCompositionId(),
                    message.getRevisionIdComposition());
            if (valid) {
                message.setCompositionId(null);
                message.setRevisionIdComposition(null);
            } else {
                result = false;
            }
        }
        if (message.getCompositionTargetId() != null) {
            var valid = cacheProvider.isCompositionDefinitionUpdated(message.getCompositionTargetId(),
                    message.getRevisionIdCompositionTarget());
            if (valid) {
                message.setCompositionTargetId(null);
                message.setRevisionIdCompositionTarget(null);
            } else {
                result = false;
            }
        }
        if (message.getInstanceId() != null) {
            var valid = cacheProvider.isInstanceUpdated(message.getInstanceId(), message.getRevisionIdInstance());
            if (valid) {
                message.setInstanceId(null);
                message.setRevisionIdInstance(null);
            } else {
                result = false;
            }
        }
        return result;
    }
}

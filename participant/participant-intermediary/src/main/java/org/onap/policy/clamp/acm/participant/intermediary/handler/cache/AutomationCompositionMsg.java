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

package org.onap.policy.clamp.acm.participant.intermediary.handler.cache;

import java.util.UUID;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessage;

@Data
public class AutomationCompositionMsg<T extends ParticipantMessage> {

    private UUID key = UUID.randomUUID();

    @Setter(AccessLevel.NONE)
    private final Consumer<T> consumer;

    @Setter(AccessLevel.NONE)
    private final T message;

    private UUID instanceId;
    private UUID revisionIdInstance;
    private UUID compositionId;
    private UUID revisionIdComposition;
    private UUID compositionTargetId;
    private UUID revisionIdCompositionTarget;

    public AutomationCompositionMsg(Consumer<T> consumer, T message) {
        this.consumer = consumer;
        this.message = message;
    }

    public void execute() {
        consumer.accept(message);
    }
}

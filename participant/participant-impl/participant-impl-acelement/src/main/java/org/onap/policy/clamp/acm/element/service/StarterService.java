/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.element.service;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;
import org.onap.policy.clamp.acm.element.handler.MessagePublisher;
import org.onap.policy.clamp.acm.element.main.parameters.AcElement;
import org.onap.policy.clamp.models.acm.messages.dmaap.element.ElementStatus;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementConfig;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementType;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.stereotype.Service;

/**
 * Starter Service.
 */
@Service
public class StarterService extends AbstractElementService implements AutoCloseable {

    private ScheduledThreadPoolExecutor timerPool;
    private ScheduledFuture<?> future;
    private ToscaConceptIdentifier receiver;
    private ToscaConceptIdentifier elementId;

    private final MessagePublisher messagePublisher;

    public StarterService(MessagePublisher messagePublisher, AcElement acElement) {
        this.messagePublisher = messagePublisher;
        this.elementId = acElement.getElementId();
    }

    @Override
    public ElementType getType() {
        return ElementType.STARTER;
    }

    /**
     * Deactivate Scheduled ThreadPool Executor.
     */
    @Override
    public void deactivate() {
        if (timerPool != null) {
            if (future != null) {
                future.cancel(true);
            }
            timerPool.shutdown();
            timerPool = null;
        }
    }

    @Override
    public void active(ElementConfig elementConfig) {
        if (timerPool != null) {
            throw new PfModelRuntimeException(Response.Status.CONFLICT, "StarterService alredy actived!");
        }
        receiver = elementConfig.getElementId();

        timerPool = new ScheduledThreadPoolExecutor(1);
        timerPool.setRemoveOnCancelPolicy(true);
        future = timerPool.scheduleAtFixedRate(this::sendMessage, elementConfig.getTimerSec(),
                elementConfig.getTimerSec(), TimeUnit.MILLISECONDS);
    }

    private void sendMessage() {
        var messasge = new ElementStatus();
        messasge.setElementId(receiver);
        // Add Tracking
        messasge.setMessage("starter: " + elementId);
        messagePublisher.publishMsg(messasge);
    }

    @Override
    public void update(ElementConfig elementConfig) {
        if (timerPool == null) {
            throw new PfModelRuntimeException(Response.Status.CONFLICT, "StarterService not actived!");
        }
        if (future != null) {
            future.cancel(true);
        }
        future = timerPool.scheduleAtFixedRate(this::sendMessage, elementConfig.getTimerSec(),
                elementConfig.getTimerSec(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws Exception {
        deactivate();
    }
}

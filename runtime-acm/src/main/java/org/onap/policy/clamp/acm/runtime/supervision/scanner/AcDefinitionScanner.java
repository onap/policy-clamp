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

package org.onap.policy.clamp.acm.runtime.supervision.scanner;

import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.document.concepts.DocMessage;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AcDefinitionScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcDefinitionScanner.class);

    private final long maxOperationWaitMs;

    private final AcDefinitionProvider acDefinitionProvider;
    private final ParticipantSyncPublisher participantSyncPublisher;

    /**
     * Constructor for instantiating AcDefinitionScanner.
     *
     * @param acDefinitionProvider the Policy Models Provider
     * @param participantSyncPublisher the Participant Sync Publisher
     * @param acRuntimeParameterGroup the parameters for the automation composition runtime
     */
    public AcDefinitionScanner(final AcDefinitionProvider acDefinitionProvider,
            final ParticipantSyncPublisher participantSyncPublisher,
            final AcRuntimeParameterGroup acRuntimeParameterGroup) {
        this.acDefinitionProvider = acDefinitionProvider;
        this.participantSyncPublisher = participantSyncPublisher;
        this.maxOperationWaitMs = acRuntimeParameterGroup.getParticipantParameters().getMaxOperationWaitMs();
    }

    private UpdateSync handlePrimeAckElement(DocMessage message, AutomationCompositionDefinition acDefinition) {
        var result = new UpdateSync();
        if (StateChangeResult.FAILED.equals(message.getStateChangeResult())) {
            acDefinition.setStateChangeResult(StateChangeResult.FAILED);
            result.setUpdated(true);
            result.setToBeSync(true);
        }
        for (var element : acDefinition.getElementStateMap().values()) {
            if (message.getParticipantId().equals(element.getParticipantId())) {
                element.setMessage(message.getMessage());
                element.setState(message.getCompositionState());
                result.setUpdated(true);
            }
        }
        return result;
    }

    private UpdateSync handleOutProperties(DocMessage message, AutomationCompositionDefinition acDefinition) {
        var elementOpt = acDefinition.getElementStateMap().values().stream()
                .filter(element -> element.getNodeTemplateId().equals(message.getAcElementDefinitionId())).findFirst();

        var result = new UpdateSync();
        if (elementOpt.isPresent()) {
            elementOpt.get().setOutProperties(message.getOutProperties());
            result.setUpdated(true);
            result.setToBeSync(true);
        }
        return result;
    }

    /**
     * Scan Message.
     *
     * @param acDefinition the AutomationComposition Definition
     * @param message the message
     */
    public UpdateSync scanMessage(AutomationCompositionDefinition acDefinition, DocMessage message) {
        return switch (message.getMessageType()) {
            case PARTICIPANT_STATUS -> handleOutProperties(message, acDefinition);
            case PARTICIPANT_PRIME_ACK -> handlePrimeAckElement(message, acDefinition);
            default -> {
                LOGGER.debug("Not valid MessageType {}", message.getMessageType());
                yield new UpdateSync();
            }
        };
    }

    /**
     * Scan an AutomationComposition Definition.
     *
     * @param acDefinition the AutomationComposition Definition
     * @param updateSync defines if true if the composition has to be saved or sync
     */
    public void scanAutomationCompositionDefinition(AutomationCompositionDefinition acDefinition,
            UpdateSync updateSync) {
        if (StateChangeResult.FAILED.equals(acDefinition.getStateChangeResult())) {
            LOGGER.debug("automation definition {} scanned, OK", acDefinition.getCompositionId());
            updateAcDefinitionState(acDefinition, updateSync);
            return;
        }

        boolean completed = true;
        var finalState = AcTypeState.PRIMING.equals(acDefinition.getState())
                || AcTypeState.PRIMED.equals(acDefinition.getState()) ? AcTypeState.PRIMED : AcTypeState.COMMISSIONED;
        for (var element : acDefinition.getElementStateMap().values()) {
            if (!finalState.equals(element.getState())) {
                completed = false;
                break;
            }
        }
        if (completed) {
            acDefinition.setState(finalState);
            acDefinition.setStateChangeResult(StateChangeResult.NO_ERROR);
            updateSync.setUpdated(true);
            updateSync.setToBeSync(true);
        } else {
            updateSync.or(handleTimeout(acDefinition));
        }
        updateAcDefinitionState(acDefinition, updateSync);
    }

    private UpdateSync handleTimeout(AutomationCompositionDefinition acDefinition) {
        var result = new UpdateSync();
        if (StateChangeResult.TIMEOUT.equals(acDefinition.getStateChangeResult())) {
            LOGGER.debug("The ac definition is in timeout {}", acDefinition.getCompositionId());
            return result;
        }
        var now = TimestampHelper.nowEpochMilli();
        var lastMsg = TimestampHelper.toEpochMilli(acDefinition.getLastMsg());
        if ((now - lastMsg) > maxOperationWaitMs) {
            LOGGER.debug("Report timeout for the ac definition {}", acDefinition.getCompositionId());
            acDefinition.setStateChangeResult(StateChangeResult.TIMEOUT);
            result.setUpdated(true);
            result.setToBeSync(true);
        }
        return result;
    }

    private void updateAcDefinitionState(AutomationCompositionDefinition acDefinition,
            UpdateSync updateSync) {
        if (updateSync.isUpdated()) {
            acDefinitionProvider.updateAcDefinitionState(acDefinition);
        }
        if (updateSync.isToBeSync()) {
            participantSyncPublisher.sendSync(acDefinition, null);
        }
    }
}

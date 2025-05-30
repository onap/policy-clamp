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
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.document.concepts.DocMessage;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimpleScanner extends AbstractScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleScanner.class);

    /**
     * Constructor for instantiating SimpleScanner.
     *
     * @param acProvider the provider to use to read automation compositions from the database
     * @param participantSyncPublisher the Participant Sync Publisher
     * @param acRuntimeParameterGroup the parameters for the automation composition runtime
     */
    public SimpleScanner(final AutomationCompositionProvider acProvider,
                         final ParticipantSyncPublisher participantSyncPublisher,
                         final AcRuntimeParameterGroup acRuntimeParameterGroup,
                         final EncryptionUtils encryptionUtils) {
        super(acProvider, participantSyncPublisher, acRuntimeParameterGroup, encryptionUtils);
    }

    /**
     * Scan Message.
     *
     * @param automationComposition the AutomationComposition
     * @param message the message
     * @return the update/sync information
     */
    public UpdateSync scanMessage(AutomationComposition automationComposition, DocMessage message) {
        return switch (message.getMessageType()) {
            case PARTICIPANT_STATUS -> handleOutProperties(automationComposition, message);
            case AUTOMATION_COMPOSITION_DEPLOY_ACK, AUTOMATION_COMPOSITION_STATECHANGE_ACK
                    -> handleAcStateChange(automationComposition, message);
            default -> {
                LOGGER.debug("Not valid MessageType {}", message.getMessageType());
                yield new UpdateSync();
            }
        };
    }

    private UpdateSync handleAcStateChange(AutomationComposition automationComposition, DocMessage message) {
        var result = new UpdateSync();
        var element = automationComposition.getElements().get(message.getInstanceElementId());
        if (element == null || !validateStateMessage(automationComposition, message)) {
            return result;
        }
        result.setUpdated(true);
        if (StateChangeResult.FAILED.equals(message.getStateChangeResult())) {
            automationComposition.setStateChangeResult(StateChangeResult.FAILED);
            result.setToBeSync(true);
        }
        element.setDeployState(message.getDeployState());
        element.setLockState(message.getLockState());
        if (message.getStage() == null) {
            element.setSubState(SubState.NONE);
        }
        element.setStage(message.getStage());
        element.setMessage(message.getMessage());
        return result;
    }

    private boolean validateStateMessage(AutomationComposition automationComposition, DocMessage message) {
        return !DeployState.DELETED.equals(message.getDeployState())
                || (DeployState.DELETING.equals(automationComposition.getDeployState()));
    }

    private UpdateSync handleOutProperties(AutomationComposition automationComposition, DocMessage message) {
        var element = automationComposition.getElements().get(message.getInstanceElementId());
        var result = new UpdateSync();
        if (element != null) {
            element.setOutProperties(message.getOutProperties());
            element.setOperationalState(message.getOperationalState());
            element.setUseState(message.getUseState());
            result.setUpdated(true);
            result.setToBeSync(true);
        }
        return result;
    }

    /**
     * Simple scan: UPDATE, PREPARE, REVIEW, MIGRATE_PRECHECKING.
     *
     * @param automationComposition the AutomationComposition
     * @param updateSync the update/sync information
     */
    public void simpleScan(final AutomationComposition automationComposition, UpdateSync updateSync) {
        var completed = automationComposition.getElements().values().stream()
                .filter(element -> AcmUtils.isInTransitionalState(element.getDeployState(), element.getLockState(),
                        element.getSubState())).findFirst().isEmpty();

        if (completed) {
            complete(automationComposition, updateSync);
        } else {
            handleTimeout(automationComposition, updateSync);
        }
    }
}

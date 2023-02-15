/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.supervision;

import io.micrometer.core.annotation.Timed;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeployAck;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles supervision of automation composition instances, so only one object of this type should be built
 * at a time.
 */
@Component
@AllArgsConstructor
public class SupervisionAcHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionAcHandler.class);

    private final AutomationCompositionProvider automationCompositionProvider;

    // Publishers for participant communication
    private final AutomationCompositionDeployPublisher automationCompositionDeployPublisher;
    private final AutomationCompositionStateChangePublisher automationCompositionStateChangePublisher;

    /**
     * Handle Deploy an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void deploy(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        AcmUtils.setCascadedState(automationComposition, DeployState.DEPLOYING, LockState.NONE);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        var startPhase = ParticipantUtils.getFirstStartPhase(automationComposition, acDefinition.getServiceTemplate());
        automationCompositionDeployPublisher.send(automationComposition, acDefinition.getServiceTemplate(), startPhase,
                true);
    }

    /**
     * Handle Undeploy an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void undeploy(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        AcmUtils.setCascadedState(automationComposition, DeployState.UNDEPLOYING, LockState.NONE);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        var startPhase = ParticipantUtils.getFirstStartPhase(automationComposition, acDefinition.getServiceTemplate());
        automationCompositionStateChangePublisher.send(automationComposition, startPhase, true);
    }

    /**
     * Handle Unlock an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void unlock(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        AcmUtils.setCascadedState(automationComposition, DeployState.DEPLOYED, LockState.UNLOCKING);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        var startPhase = ParticipantUtils.getFirstStartPhase(automationComposition, acDefinition.getServiceTemplate());
        automationCompositionStateChangePublisher.send(automationComposition, startPhase, true);
    }

    /**
     * Handle Lock an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void lock(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        AcmUtils.setCascadedState(automationComposition, DeployState.DEPLOYED, LockState.LOCKING);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        var startPhase = ParticipantUtils.getFirstStartPhase(automationComposition, acDefinition.getServiceTemplate());
        automationCompositionStateChangePublisher.send(automationComposition, startPhase, true);
    }

    /**
     * Handle a AutomationComposition deploy acknowledge message from a participant.
     *
     * @param automationCompositionAckMessage the AutomationCompositionAck message received from a participant
     */
    @MessageIntercept
    @Timed(
            value = "listener.automation_composition_deploy_ack",
            description = "AUTOMATION_COMPOSITION_DEPLOY_ACK messages received")
    public void handleAutomationCompositionUpdateAckMessage(
            AutomationCompositionDeployAck automationCompositionAckMessage) {
        LOGGER.debug("AutomationComposition Update Ack message received {}", automationCompositionAckMessage);
        setAcElementStateInDb(automationCompositionAckMessage);
    }

    /**
     * Handle a AutomationComposition statechange acknowledge message from a participant.
     *
     * @param automationCompositionAckMessage the AutomationCompositionAck message received from a participant
     */
    @MessageIntercept
    @Timed(
            value = "listener.automation_composition_statechange_ack",
            description = "AUTOMATION_COMPOSITION_STATECHANGE_ACK messages received")
    public void handleAutomationCompositionStateChangeAckMessage(
            AutomationCompositionDeployAck automationCompositionAckMessage) {
        LOGGER.debug("AutomationComposition StateChange Ack message received {}", automationCompositionAckMessage);
        setAcElementStateInDb(automationCompositionAckMessage);
    }

    private void setAcElementStateInDb(AutomationCompositionDeployAck automationCompositionAckMessage) {
        if (automationCompositionAckMessage.getAutomationCompositionResultMap() != null) {
            var automationComposition = automationCompositionProvider
                    .findAutomationComposition(automationCompositionAckMessage.getAutomationCompositionId());
            if (automationComposition.isPresent()) {
                var updated = updateState(automationComposition.get(),
                        automationCompositionAckMessage.getAutomationCompositionResultMap().entrySet());
                if (updated) {
                    automationCompositionProvider.updateAutomationComposition(automationComposition.get());
                }
            } else {
                LOGGER.warn("AutomationComposition not found in database {}",
                        automationCompositionAckMessage.getAutomationCompositionId());
            }
        }
    }

    private boolean updateState(AutomationComposition automationComposition,
            Set<Map.Entry<UUID, AcElementDeployAck>> automationCompositionResultSet) {
        var updated = false;
        for (var acElementAck : automationCompositionResultSet) {
            var element = automationComposition.getElements().get(acElementAck.getKey());
            if (element != null) {
                element.setDeployState(acElementAck.getValue().getDeployState());
                element.setLockState(acElementAck.getValue().getLockState());
                updated = true;
            }
        }
        return updated;
    }
}

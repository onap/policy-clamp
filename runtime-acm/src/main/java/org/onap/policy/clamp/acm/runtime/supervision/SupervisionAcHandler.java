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

package org.onap.policy.clamp.acm.runtime.supervision;

import io.micrometer.core.annotation.Timed;
import io.opentelemetry.context.Context;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AcElementPropertiesPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AcPreparePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionMigrationPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.common.acm.utils.AcmThreadFactory;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeployAck;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.MigrationState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.onap.policy.models.base.PfModelRuntimeException;
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
    private final AcElementPropertiesPublisher acElementPropertiesPublisher;
    private final AutomationCompositionMigrationPublisher acCompositionMigrationPublisher;
    private final AcPreparePublisher acPreparePublisher;
    private final MessageProvider messageProvider;
    private final EncryptionUtils encryptionUtils;

    private final ExecutorService executor =
            Context.taskWrapping(Executors.newFixedThreadPool(1, new AcmThreadFactory()));

    /**
     * Handle Deploy an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void deploy(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        LOGGER.info("Deployment request received for instanceID: {}", automationComposition.getInstanceId());

        var elements = automationComposition.getElements().values();
        // check if elements are in a valid state to be deployed
        elements.stream().filter(element -> !MigrationState.DEFAULT.equals(element.getMigrationState()))
            .findAny().ifPresent(element -> {
                var msg = String.format("Instance cannot be deployed; There are elements in an invalid Migration state."
                    + "(ElementId: %s, MigrationState: %s)", element.getId(), element.getMigrationState());
                LOGGER.warn(msg);
                throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, msg);
            });

        if (StateChangeResult.FAILED.equals(automationComposition.getStateChangeResult())
                && DeployState.DEPLOYING.equals(automationComposition.getDeployState())
                && automationComposition.getElements().size() > 1) {
            automationComposition.setLastMsg(TimestampHelper.now());

            for (var element : elements) {
                if (!DeployState.DEPLOYED.equals(element.getDeployState())) {
                    element.setDeployState(DeployState.DEPLOYING);
                    element.setMessage(null);
                }
            }
        } else {
            AcmUtils.setCascadedState(automationComposition, DeployState.DEPLOYING, LockState.NONE);
        }
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        var startPhase = ParticipantUtils.getFirstStartPhase(automationComposition, acDefinition.getServiceTemplate());
        automationComposition.setPhase(startPhase);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        executor.execute(
            () -> {
                var acToSend = new AutomationComposition(automationComposition);
                encryptionUtils.decryptInstanceProperties(acToSend);
                automationCompositionDeployPublisher.send(acToSend, startPhase, true, acDefinition.getRevisionId());
            });
    }

    /**
     * Handle Undeploy an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void undeploy(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        LOGGER.info("Undeployment request received for instanceID: {}", automationComposition.getInstanceId());
        if (StateChangeResult.FAILED.equals(automationComposition.getStateChangeResult())
                && DeployState.UNDEPLOYING.equals(automationComposition.getDeployState())
                && automationComposition.getElements().size() > 1) {
            automationComposition.setLastMsg(TimestampHelper.now());
            for (var element : automationComposition.getElements().values()) {
                if (!DeployState.UNDEPLOYED.equals(element.getDeployState())) {
                    element.setDeployState(DeployState.UNDEPLOYING);
                    element.setMessage(null);
                }
            }
        } else {
            AcmUtils.setCascadedState(automationComposition, DeployState.UNDEPLOYING, LockState.NONE);
        }
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        automationComposition.setCompositionTargetId(null);
        var startPhase = ParticipantUtils.getFirstStartPhase(automationComposition, acDefinition.getServiceTemplate());
        automationComposition.setPhase(startPhase);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        executor.execute(() -> automationCompositionStateChangePublisher.send(automationComposition,
                    startPhase, true, acDefinition.getRevisionId()));
    }

    /**
     * Handle Unlock an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void unlock(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        LOGGER.info("Unlock request received for instanceID: {}", automationComposition.getInstanceId());
        if (StateChangeResult.FAILED.equals(automationComposition.getStateChangeResult())
                && LockState.UNLOCKING.equals(automationComposition.getLockState())
                && automationComposition.getElements().size() > 1) {
            automationComposition.setLastMsg(TimestampHelper.now());
            for (var element : automationComposition.getElements().values()) {
                if (!LockState.UNLOCKED.equals(element.getLockState())) {
                    element.setLockState(LockState.UNLOCKING);
                }
            }
        } else {
            AcmUtils.setCascadedState(automationComposition, DeployState.DEPLOYED, LockState.UNLOCKING);
        }
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        var startPhase = ParticipantUtils.getFirstStartPhase(automationComposition, acDefinition.getServiceTemplate());
        automationComposition.setPhase(startPhase);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        executor.execute(
            () -> automationCompositionStateChangePublisher.send(automationComposition,
                    startPhase, true, acDefinition.getRevisionId()));
    }

    /**
     * Handle prepare Pre Deploy an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void prepare(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        LOGGER.info("Prepare pre-deploy request received for instanceID: {}", automationComposition.getInstanceId());
        AcmUtils.setCascadedState(automationComposition, DeployState.UNDEPLOYED, LockState.NONE, SubState.PREPARING);
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        var stage = ParticipantUtils.getFirstStage(automationComposition, acDefinition.getServiceTemplate());
        automationComposition.setPhase(stage);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        executor.execute(() -> {
            var acToSend = new AutomationComposition(automationComposition);
            encryptionUtils.decryptInstanceProperties(acToSend);
            acPreparePublisher.sendPrepare(acToSend, stage, acDefinition.getRevisionId());
        });
    }

    /**
     * Handle a prepare Post Deploy an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void review(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        LOGGER.info("Prepare post-deploy request received for instanceID: {}", automationComposition.getInstanceId());
        AcmUtils.setCascadedState(automationComposition, DeployState.DEPLOYED, LockState.LOCKED, SubState.REVIEWING);
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        executor.execute(() -> acPreparePublisher.sendReview(automationComposition, acDefinition.getRevisionId()));
    }

    /**
     * Handle Lock an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void lock(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        LOGGER.info("Lock request received for instanceID: {}", automationComposition.getInstanceId());
        if (StateChangeResult.FAILED.equals(automationComposition.getStateChangeResult())
                && LockState.LOCKING.equals(automationComposition.getLockState())
                && automationComposition.getElements().size() > 1) {
            automationComposition.setLastMsg(TimestampHelper.now());
            for (var element : automationComposition.getElements().values()) {
                if (!LockState.LOCKED.equals(element.getLockState())) {
                    element.setLockState(LockState.LOCKING);
                }
            }
        } else {
            AcmUtils.setCascadedState(automationComposition, DeployState.DEPLOYED, LockState.LOCKING);
        }
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        var startPhase = ParticipantUtils.getFirstStartPhase(automationComposition, acDefinition.getServiceTemplate());
        automationComposition.setPhase(startPhase);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        executor.execute(
            () -> automationCompositionStateChangePublisher.send(automationComposition,
                    startPhase, true, acDefinition.getRevisionId()));
    }

    /**
     * Handle Element property update on a deployed instance.
     *
     * @param automationComposition the AutomationComposition
     * @param revisionIdComposition the last Update from Composition
     */
    public void update(AutomationComposition automationComposition, UUID revisionIdComposition) {
        executor.execute(
            () -> {
                encryptionUtils.decryptInstanceProperties(automationComposition);
                acElementPropertiesPublisher.send(automationComposition, revisionIdComposition);
            });
    }

    /**
     * Handle Delete an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void delete(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        AcmUtils.setCascadedState(automationComposition, DeployState.DELETING, LockState.NONE);
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        var startPhase = ParticipantUtils.getFirstStartPhase(automationComposition, acDefinition.getServiceTemplate());
        automationComposition.setPhase(startPhase);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        executor.execute(
            () -> automationCompositionStateChangePublisher.send(
                    automationComposition, startPhase, true, acDefinition.getRevisionId()));
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
        setAcElementStateInDb(automationCompositionAckMessage);
    }

    private void setAcElementStateInDb(AutomationCompositionDeployAck automationCompositionAckMessage) {
        if (!validateMessage(automationCompositionAckMessage)) {
            return;
        }

        var automationCompositionOpt = automationCompositionProvider
                .findAutomationComposition(automationCompositionAckMessage.getAutomationCompositionId());
        if (automationCompositionOpt.isEmpty()) {
            LOGGER.error("AutomationComposition not found in database {}",
                    automationCompositionAckMessage.getAutomationCompositionId());
            return;
        }
        var automationComposition = automationCompositionOpt.get();

        if (automationCompositionAckMessage.getAutomationCompositionResultMap() == null
                || automationCompositionAckMessage.getAutomationCompositionResultMap().isEmpty()) {
            // scenario automationComposition has never been deployed
            automationCompositionAckMessage.setAutomationCompositionResultMap(new HashMap<>());
            for (var element : automationComposition.getElements().values()) {
                if (element.getParticipantId().equals(automationCompositionAckMessage.getParticipantId())) {
                    var acElement =
                            new AcElementDeployAck(DeployState.DELETED, LockState.NONE, null, null, Map.of(), true, "");
                    automationCompositionAckMessage.getAutomationCompositionResultMap().put(element.getId(), acElement);
                }
            }
        }
        messageProvider.save(automationCompositionAckMessage);
    }

    private boolean validateMessage(AutomationCompositionDeployAck acAckMessage) {
        if (acAckMessage.getAutomationCompositionId() == null
                || acAckMessage.getStateChangeResult() == null) {
            LOGGER.error("Not valid AutomationCompositionDeployAck message");
            return false;
        }
        if (!StateChangeResult.NO_ERROR.equals(acAckMessage.getStateChangeResult())
                && !StateChangeResult.FAILED.equals(acAckMessage.getStateChangeResult())) {
            LOGGER.error("Not valid AutomationCompositionDeployAck message, stateChangeResult is not valid {} ",
                    acAckMessage.getStateChangeResult());
            return false;
        }

        if ((acAckMessage.getStage() == null)
            && (acAckMessage.getAutomationCompositionResultMap() != null)) {
            for (var el : acAckMessage.getAutomationCompositionResultMap().values()) {
                if (AcmUtils.isInTransitionalState(el.getDeployState(), el.getLockState(), SubState.NONE)) {
                    LOGGER.error("Not valid AutomationCompositionDeployAck message, states are not valid");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Handle Migration of an AutomationComposition instance to other ACM Definition.
     *
     * @param automationComposition the AutomationComposition
     * @param revisionIdComposition the last Update from Composition
     * @param revisionIdCompositionTarget the last Update from Composition Target
     */
    public void migrate(AutomationComposition automationComposition, UUID revisionIdComposition,
                        UUID revisionIdCompositionTarget, List<AutomationCompositionElement> removedElements) {
        executor.execute(() -> {
            encryptionUtils.decryptInstanceProperties(automationComposition);
            acCompositionMigrationPublisher.send(automationComposition, automationComposition.getPhase(),
                    revisionIdComposition, revisionIdCompositionTarget, removedElements);
        });
    }

    /**
     * Handle Migration precheck of an AutomationComposition instance to other ACM Definition.
     *
     * @param automationComposition the AutomationComposition
     * @param revisionIdComposition the last Update from Composition
     * @param revisionIdCompositionTarget the last Update from Composition Target
     */
    public void migratePrecheck(AutomationComposition automationComposition, UUID revisionIdComposition,
            UUID revisionIdCompositionTarget, List<AutomationCompositionElement> removedElements) {
        executor.execute(() -> acCompositionMigrationPublisher.send(automationComposition, 0,
                revisionIdComposition, revisionIdCompositionTarget, removedElements));
    }
}

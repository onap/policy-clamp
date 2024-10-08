/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AcElementPropertiesPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AcPreparePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionMigrationPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeployAck;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
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
    private final AcDefinitionProvider acDefinitionProvider;

    // Publishers for participant communication
    private final AutomationCompositionDeployPublisher automationCompositionDeployPublisher;
    private final AutomationCompositionStateChangePublisher automationCompositionStateChangePublisher;
    private final AcElementPropertiesPublisher acElementPropertiesPublisher;
    private final AutomationCompositionMigrationPublisher acCompositionMigrationPublisher;
    private final ParticipantSyncPublisher participantSyncPublisher;
    private final AcPreparePublisher acPreparePublisher;

    private final ExecutorService executor = Context.taskWrapping(Executors.newFixedThreadPool(1));

    /**
     * Handle Deploy an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void deploy(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        if (StateChangeResult.FAILED.equals(automationComposition.getStateChangeResult())) {
            automationComposition.setDeployState(DeployState.DEPLOYING);
            for (var element : automationComposition.getElements().values()) {
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
            () -> automationCompositionDeployPublisher.send(automationComposition, acDefinition.getServiceTemplate(),
                startPhase, true));
    }

    /**
     * Handle Undeploy an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void undeploy(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        if (StateChangeResult.FAILED.equals(automationComposition.getStateChangeResult())) {
            automationComposition.setDeployState(DeployState.UNDEPLOYING);
            for (var element : automationComposition.getElements().values()) {
                if (!DeployState.UNDEPLOYED.equals(element.getDeployState())) {
                    element.setDeployState(DeployState.UNDEPLOYING);
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
        executor.execute(
            () -> automationCompositionStateChangePublisher.send(automationComposition, startPhase, true));
    }

    /**
     * Handle Unlock an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void unlock(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        if (StateChangeResult.FAILED.equals(automationComposition.getStateChangeResult())) {
            automationComposition.setLockState(LockState.UNLOCKING);
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
            () -> automationCompositionStateChangePublisher.send(automationComposition, startPhase, true));
    }

    /**
     * Handle prepare Pre Deploy an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     */
    public void prepare(AutomationComposition automationComposition) {
        AcmUtils.setCascadedState(automationComposition, DeployState.UNDEPLOYED, LockState.NONE, SubState.PREPARING);
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        executor.execute(() -> acPreparePublisher.sendPrepare(automationComposition));
    }

    /**
     * Handle prepare Post Deploy an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     */
    public void review(AutomationComposition automationComposition) {
        AcmUtils.setCascadedState(automationComposition, DeployState.DEPLOYED, LockState.LOCKED, SubState.REVIEWING);
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        executor.execute(() -> acPreparePublisher.sendRevew(automationComposition));
    }

    /**
     * Handle Lock an AutomationComposition instance.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void lock(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        if (StateChangeResult.FAILED.equals(automationComposition.getStateChangeResult())) {
            automationComposition.setLockState(LockState.LOCKING);
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
            () -> automationCompositionStateChangePublisher.send(automationComposition, startPhase, true));
    }

    /**
     * Handle Element property update on a deployed instance.
     *
     * @param automationComposition the AutomationComposition
     */
    public void update(AutomationComposition automationComposition) {
        AcmUtils.setCascadedState(automationComposition, DeployState.UPDATING, automationComposition.getLockState());
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        executor.execute(
            () -> acElementPropertiesPublisher.send(automationComposition));
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
            () -> automationCompositionStateChangePublisher.send(automationComposition, startPhase, true));
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
            if (DeployState.DELETING.equals(automationComposition.getDeployState())) {
                deleteAcInstance(automationComposition, automationCompositionAckMessage.getParticipantId());
            } else {
                LOGGER.warn("Empty AutomationCompositionResultMap  {} {}",
                        automationCompositionAckMessage.getAutomationCompositionId(),
                        automationCompositionAckMessage.getMessage());
            }
            return;
        }

        var updated = updateState(automationComposition,
                automationCompositionAckMessage.getAutomationCompositionResultMap().entrySet(),
                automationCompositionAckMessage.getStateChangeResult(), automationCompositionAckMessage.getStage());
        if (updated) {
            automationComposition = automationCompositionProvider.updateAcState(automationComposition);
            var acDefinition = acDefinitionProvider.getAcDefinition(automationComposition.getCompositionId());
            participantSyncPublisher.sendSync(acDefinition.getServiceTemplate(), automationComposition);
        }
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

        if (acAckMessage.getStage() == null) {
            for (var el : acAckMessage.getAutomationCompositionResultMap().values()) {
                if (AcmUtils.isInTransitionalState(el.getDeployState(), el.getLockState(), SubState.NONE)) {
                    LOGGER.error("Not valid AutomationCompositionDeployAck message, states are not valid");
                    return false;
                }
            }
        }
        return true;
    }

    private void deleteAcInstance(AutomationComposition automationComposition, UUID participantId) {
        // scenario when Automation Composition instance has never been deployed
        for (var element : automationComposition.getElements().values()) {
            if (element.getParticipantId().equals(participantId)) {
                element.setDeployState(DeployState.DELETED);
                automationCompositionProvider.updateAutomationCompositionElement(element);
            }
        }
    }

    private boolean updateState(AutomationComposition automationComposition,
            Set<Map.Entry<UUID, AcElementDeployAck>> automationCompositionResultSet,
            StateChangeResult stateChangeResult, Integer stage) {
        var updated = false;
        boolean inProgress = !StateChangeResult.FAILED.equals(automationComposition.getStateChangeResult());
        if (inProgress && !stateChangeResult.equals(automationComposition.getStateChangeResult())) {
            automationComposition.setStateChangeResult(stateChangeResult);
            updated = true;
        }

        for (var acElementAck : automationCompositionResultSet) {
            var element = automationComposition.getElements().get(acElementAck.getKey());
            if (element != null) {
                element.setMessage(AcmUtils.validatedMessage(acElementAck.getValue().getMessage()));
                if (stage == null) {
                    element.setSubState(SubState.NONE);
                }
                element.setDeployState(acElementAck.getValue().getDeployState());
                element.setLockState(acElementAck.getValue().getLockState());
                element.setStage(stage);
                automationCompositionProvider.updateAutomationCompositionElement(element);
            }
        }

        return updated;
    }

    /**
     * Handle Migration of an AutomationComposition instance to other ACM Definition.
     *
     * @param automationComposition the AutomationComposition
     * @param serviceTemplate the ServiceTemplate
     */
    public void migrate(AutomationComposition automationComposition, ToscaServiceTemplate serviceTemplate) {
        AcmUtils.setCascadedState(automationComposition, DeployState.MIGRATING, LockState.LOCKED);
        var stage = ParticipantUtils.getFirstStage(automationComposition, serviceTemplate);
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        automationComposition.setPhase(stage);
        executor.execute(() -> acCompositionMigrationPublisher.send(automationComposition, stage));
    }

    /**
     * Handle Migration precheck of an AutomationComposition instance to other ACM Definition.
     *
     * @param automationComposition the AutomationComposition
     */
    public void migratePrecheck(AutomationComposition automationComposition) {
        executor.execute(() -> acCompositionMigrationPublisher.send(automationComposition, 0));
    }
}

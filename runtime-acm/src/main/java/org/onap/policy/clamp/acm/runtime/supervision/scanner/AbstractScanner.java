/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
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
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractScanner {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractScanner.class);

    protected final long maxStatusWaitMs;

    protected final AutomationCompositionProvider acProvider;
    private final ParticipantSyncPublisher participantSyncPublisher;
    private final EncryptionUtils encryptionUtils;

    protected AbstractScanner(final AutomationCompositionProvider acProvider,
            final ParticipantSyncPublisher participantSyncPublisher,
            final AcRuntimeParameterGroup acRuntimeParameterGroup, final EncryptionUtils encryptionUtils) {
        this.acProvider = acProvider;
        this.participantSyncPublisher = participantSyncPublisher;
        this.maxStatusWaitMs = acRuntimeParameterGroup.getParticipantParameters().getMaxStatusWaitMs();
        this.encryptionUtils = encryptionUtils;
    }

    protected void complete(final AutomationComposition automationComposition, UpdateSync updateSync) {
        LOGGER.debug("automation composition scan: transition state {} {} {} completed",
                automationComposition.getDeployState(), automationComposition.getLockState(),
                automationComposition.getSubState());

        var deployState = automationComposition.getDeployState();
        if (DeployState.MIGRATING.equals(automationComposition.getDeployState())) {
            // migration scenario
            automationComposition.setCompositionId(automationComposition.getCompositionTargetId());
            automationComposition.setCompositionTargetId(null);
        }
        automationComposition.setDeployState(AcmUtils.deployCompleted(deployState));
        automationComposition.setLockState(AcmUtils.lockCompleted(deployState, automationComposition.getLockState()));
        automationComposition.setPhase(null);
        automationComposition.setSubState(SubState.NONE);
        automationComposition.setPrecheck(null);
        if (StateChangeResult.TIMEOUT.equals(automationComposition.getStateChangeResult())) {
            automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        }
        if (DeployState.DELETED.equals(automationComposition.getDeployState())) {
            updateSync.setToBeDelete(true);
            updateSync.setUpdated(false);
        } else {
            updateSync.setUpdated(true);
        }
        updateSync.setToBeSync(true);
        saveAndSync(automationComposition, updateSync);
    }

    protected void savePhase(AutomationComposition automationComposition, int startPhase) {
        automationComposition.setLastMsg(TimestampHelper.now());
        automationComposition.setPhase(startPhase);
    }

    protected void handleTimeout(AutomationComposition automationComposition, UpdateSync updateSync) {
        LOGGER.debug("automation composition scan: transition from state {} to {} {} not completed",
                automationComposition.getDeployState(), automationComposition.getLockState(),
                automationComposition.getSubState());

        if (StateChangeResult.TIMEOUT.equals(automationComposition.getStateChangeResult())) {
            LOGGER.debug("The ac instance is in timeout {}", automationComposition.getInstanceId());
            saveAndSync(automationComposition, updateSync);
            return;
        }
        var now = TimestampHelper.nowEpochMilli();
        var lastMsg = TimestampHelper.toEpochMilli(automationComposition.getLastMsg());
        if ((now - lastMsg) > maxStatusWaitMs) {
            LOGGER.debug("Report timeout for the ac instance {}", automationComposition.getInstanceId());
            automationComposition.setStateChangeResult(StateChangeResult.TIMEOUT);
            updateSync.setUpdated(true);
            updateSync.setToBeSync(true);
        }
        saveAndSync(automationComposition, updateSync);
    }

    /**
     * Save AutomationComposition and Sync.
     *
     * @param automationComposition the AutomationComposition
     * @param updateSync the update/sync information
     */
    public void saveAndSync(AutomationComposition automationComposition, UpdateSync updateSync) {
        if (updateSync.isUpdated()) {
            acProvider.updateAutomationComposition(automationComposition);
        }
        if (updateSync.isToBeDelete()) {
            acProvider.deleteAutomationComposition(automationComposition.getInstanceId());
        }
        if (updateSync.isToBeSync()) {
            decryptInstanceProperties(automationComposition);
            participantSyncPublisher.sendSync(automationComposition);
        }
    }

    protected void decryptInstanceProperties(AutomationComposition automationComposition) {
        if (encryptionUtils.encryptionEnabled()) {
            encryptionUtils.findAndDecryptSensitiveData(automationComposition);
        }
    }
}

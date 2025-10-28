/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AcmStateUtils {

    /**
     * Return true if DeployState, LockState and SubState are in a Transitional State.
     *
     * @param deployState the DeployState
     * @param lockState the LockState
     * @param subState the SubState
     * @return true if there is a state in a Transitional State
     */
    public static boolean isInTransitionalState(DeployState deployState, LockState lockState, SubState subState) {
        return DeployState.DEPLOYING.equals(deployState) || DeployState.UNDEPLOYING.equals(deployState)
                || LockState.LOCKING.equals(lockState) || LockState.UNLOCKING.equals(lockState)
                || DeployState.DELETING.equals(deployState) || DeployState.UPDATING.equals(deployState)
                || DeployState.MIGRATING.equals(deployState) || DeployState.MIGRATION_REVERTING.equals(deployState)
                || !SubState.NONE.equals(subState);
    }

    /**
     * Return true if AcTypeState is in a Transitional State.
     *
     * @param compositionState the AcTypeState
     * @return true if the state in a Transitional State
     */
    public static boolean isInTransitionalState(AcTypeState compositionState) {
        return AcTypeState.PRIMING.equals(compositionState)
                || AcTypeState.DEPRIMING.equals(compositionState);
    }

    /**
     * Get DeployOrder from transitional DeployState.
     *
     * @param deployState the Deploy State
     * @return the DeployOrder
     */
    public static DeployOrder stateDeployToOrder(DeployState deployState) {
        return switch (deployState) {
            case DEPLOYING -> DeployOrder.DEPLOY;
            case UNDEPLOYING -> DeployOrder.UNDEPLOY;
            case DELETING -> DeployOrder.DELETE;
            default -> DeployOrder.NONE;
        };
    }

    /**
     * Get LockOrder from transitional LockState.
     *
     * @param lockState the Lock State
     * @return the LockOrder
     */
    public static LockOrder stateLockToOrder(LockState lockState) {
        if (LockState.LOCKING.equals(lockState)) {
            return LockOrder.LOCK;
        } else if (LockState.UNLOCKING.equals(lockState)) {
            return LockOrder.UNLOCK;
        }
        return LockOrder.NONE;
    }

    /**
     * Get final DeployState from transitional DeployState.
     *
     * @param deployState the DeployState
     * @return the DeployState
     */
    public static DeployState deployCompleted(DeployState deployState) {
        return switch (deployState) {
            case MIGRATING, MIGRATION_REVERTING, UPDATING, DEPLOYING -> DeployState.DEPLOYED;
            case UNDEPLOYING -> DeployState.UNDEPLOYED;
            case DELETING -> DeployState.DELETED;
            default -> deployState;
        };
    }

    /**
     * Get final LockState from transitional LockState.
     *
     * @param lockState the LockState
     * @return the LockState
     */
    public static LockState lockCompleted(DeployState deployState, LockState lockState) {
        if (LockState.LOCKING.equals(lockState) || DeployState.DEPLOYING.equals(deployState)) {
            return LockState.LOCKED;
        } else if (LockState.UNLOCKING.equals(lockState)) {
            return LockState.UNLOCKED;
        } else if (DeployState.UNDEPLOYING.equals(deployState)) {
            return LockState.NONE;
        }
        return lockState;
    }

    /**
     * Return true if transition states is Forward.
     *
     * @param deployState the DeployState
     * @param lockState the LockState
     * @return true if transition is Forward
     */
    public static boolean isForward(DeployState deployState, LockState lockState) {
        return DeployState.DEPLOYING.equals(deployState) || LockState.UNLOCKING.equals(lockState);
    }

    /**
     * Return true if migration states is Forward.
     *
     * @param deployState the DeployState
     * @return true if migration is Forward
     */
    public static boolean isMigrationForward(DeployState deployState) {
        return DeployState.MIGRATING.equals(deployState);
    }

    /**
     * Return true if the transition is MIGRATING or MIGRATION_REVERTING.
     *
     * @param deployState the DeployState
     * @return true if the transition is MIGRATING or MIGRATION_REVERTING
     */
    public static boolean isMigrating(DeployState deployState) {
        return DeployState.MIGRATING.equals(deployState) || DeployState.MIGRATION_REVERTING.equals(deployState);
    }

    /**
     * Set the states on the automation composition and on all its automation composition elements.
     *
     * @param deployState the DeployState we want the automation composition to transition to
     * @param lockState the LockState we want the automation composition to transition to
     */
    public static void setCascadedState(final AutomationComposition automationComposition,
            final DeployState deployState, final LockState lockState) {
        setCascadedState(automationComposition, deployState, lockState, SubState.NONE);
    }

    /**
     /**
     * Set the states on the automation composition and on all its automation composition elements.
     *
     * @param deployState the DeployState we want the automation composition to transition to
     * @param lockState the LockState we want the automation composition to transition to
     * @param subState the SubState we want the automation composition to transition to
     */
    public static void setCascadedState(final AutomationComposition automationComposition,
            final DeployState deployState, final LockState lockState, final SubState subState) {
        automationComposition.setDeployState(deployState);
        automationComposition.setLockState(lockState);
        automationComposition.setLastMsg(TimestampHelper.now());
        automationComposition.setSubState(subState);

        if (MapUtils.isEmpty(automationComposition.getElements())) {
            return;
        }

        for (var element : automationComposition.getElements().values()) {
            element.setDeployState(deployState);
            element.setLockState(lockState);
            element.setSubState(subState);
            element.setMessage(null);
            element.setStage(null);
        }
    }
}

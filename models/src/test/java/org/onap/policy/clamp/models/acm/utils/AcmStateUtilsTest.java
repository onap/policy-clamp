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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;

class AcmStateUtilsTest {

    @Test
    void testIsInTransitionalState() {
        assertFalse(AcmStateUtils.isInTransitionalState(DeployState.DEPLOYED, LockState.LOCKED, SubState.NONE));
        assertTrue(AcmStateUtils.isInTransitionalState(DeployState.DEPLOYING, LockState.NONE, SubState.NONE));
        assertTrue(AcmStateUtils.isInTransitionalState(DeployState.UNDEPLOYING, LockState.NONE, SubState.NONE));
        assertTrue(AcmStateUtils.isInTransitionalState(DeployState.DEPLOYED, LockState.LOCKING, SubState.NONE));
        assertTrue(AcmStateUtils.isInTransitionalState(DeployState.DEPLOYED, LockState.UNLOCKING, SubState.NONE));
        assertTrue(AcmStateUtils.isInTransitionalState(DeployState.DELETING, LockState.NONE, SubState.NONE));
        assertTrue(AcmStateUtils.isInTransitionalState(DeployState.UPDATING, LockState.LOCKED, SubState.NONE));
        assertTrue(AcmStateUtils.isInTransitionalState(DeployState.MIGRATING, LockState.LOCKED, SubState.NONE));
        assertTrue(AcmStateUtils.isInTransitionalState(DeployState.MIGRATION_REVERTING, LockState.LOCKED,
                SubState.NONE));
        assertTrue(AcmStateUtils.isInTransitionalState(DeployState.DEPLOYED, LockState.LOCKED,
                SubState.MIGRATION_PRECHECKING));
    }

    @Test
    void testCompositionIsInTransitionalState() {
        assertTrue(AcmStateUtils.isInTransitionalState(AcTypeState.PRIMING));
        assertTrue(AcmStateUtils.isInTransitionalState(AcTypeState.DEPRIMING));
        assertFalse(AcmStateUtils.isInTransitionalState(AcTypeState.PRIMED));
        assertFalse(AcmStateUtils.isInTransitionalState(AcTypeState.COMMISSIONED));
    }

    @Test
    void testStateDeployToOrder() {
        // from transitional state to order state
        assertEquals(DeployOrder.DEPLOY, AcmStateUtils.stateDeployToOrder(DeployState.DEPLOYING));
        assertEquals(DeployOrder.UNDEPLOY, AcmStateUtils.stateDeployToOrder(DeployState.UNDEPLOYING));
        assertEquals(DeployOrder.DELETE, AcmStateUtils.stateDeployToOrder(DeployState.DELETING));
        assertEquals(DeployOrder.NONE, AcmStateUtils.stateDeployToOrder(DeployState.DEPLOYED));
    }

    @Test
    void testStateLockToOrder() {
        // from transitional state to order state
        assertEquals(LockOrder.LOCK, AcmStateUtils.stateLockToOrder(LockState.LOCKING));
        assertEquals(LockOrder.UNLOCK, AcmStateUtils.stateLockToOrder(LockState.UNLOCKING));
        assertEquals(LockOrder.NONE, AcmStateUtils.stateLockToOrder(LockState.NONE));
    }

    @Test
    void testDeployCompleted() {
        // from transitional state to final state
        assertEquals(DeployState.DEPLOYED, AcmStateUtils.deployCompleted(DeployState.DEPLOYING));
        assertEquals(DeployState.UNDEPLOYED, AcmStateUtils.deployCompleted(DeployState.UNDEPLOYING));
        assertEquals(DeployState.DEPLOYED, AcmStateUtils.deployCompleted(DeployState.DEPLOYED));
        assertEquals(DeployState.DELETED, AcmStateUtils.deployCompleted(DeployState.DELETING));
    }

    @Test
    void testLockCompleted() {
        // from transitional state to final state
        assertEquals(LockState.LOCKED, AcmStateUtils.lockCompleted(DeployState.DEPLOYING, LockState.NONE));
        assertEquals(LockState.LOCKED, AcmStateUtils.lockCompleted(DeployState.DEPLOYED, LockState.LOCKING));
        assertEquals(LockState.UNLOCKED, AcmStateUtils.lockCompleted(DeployState.DEPLOYED, LockState.UNLOCKING));
        assertEquals(LockState.NONE, AcmStateUtils.lockCompleted(DeployState.UNDEPLOYING, LockState.LOCKED));
    }

    @Test
    void testIsForward() {
        assertTrue(AcmStateUtils.isForward(DeployState.DEPLOYING, LockState.NONE));
        assertTrue(AcmStateUtils.isForward(DeployState.DEPLOYED, LockState.UNLOCKING));
        assertFalse(AcmStateUtils.isForward(DeployState.DEPLOYED, LockState.LOCKING));
        assertFalse(AcmStateUtils.isForward(DeployState.UNDEPLOYING, LockState.LOCKED));
    }

    @Test
    void testIsMigratingForward() {
        assertTrue(AcmStateUtils.isMigrationForward(DeployState.MIGRATING));
        assertFalse(AcmStateUtils.isMigrationForward(DeployState.MIGRATION_REVERTING));
    }

    @Test
    void testIsMigrating() {
        assertTrue(AcmStateUtils.isMigrating(DeployState.MIGRATING));
        assertTrue(AcmStateUtils.isMigrating(DeployState.MIGRATION_REVERTING));
        assertFalse(AcmStateUtils.isMigrating(DeployState.DEPLOYING));
        assertFalse(AcmStateUtils.isMigrating(DeployState.DEPLOYED));
    }
}

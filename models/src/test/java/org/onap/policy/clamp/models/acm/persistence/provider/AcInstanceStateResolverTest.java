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

package org.onap.policy.clamp.models.acm.persistence.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.SubOrder;

class AcInstanceStateResolverTest {

    @Test
    void testResolve() {
        var acTypeStateResolver = new AcInstanceStateResolver();
        // deploy
        var result = acTypeStateResolver.resolve(DeployOrder.DEPLOY, LockOrder.NONE, SubOrder.NONE,
            DeployState.UNDEPLOYED, LockState.NONE, SubState.NONE, StateChangeResult.NO_ERROR);
        assertThat(result).isEqualTo(AcInstanceStateResolver.DEPLOY);

        // undeploy
        result = acTypeStateResolver.resolve(DeployOrder.UNDEPLOY, LockOrder.NONE, SubOrder.NONE,
            DeployState.DEPLOYED, LockState.LOCKED, SubState.NONE, StateChangeResult.NO_ERROR);
        assertThat(result).isEqualTo(AcInstanceStateResolver.UNDEPLOY);

        // unlock
        result = acTypeStateResolver.resolve(DeployOrder.NONE, LockOrder.UNLOCK, SubOrder.NONE,
            DeployState.DEPLOYED, LockState.LOCKED, SubState.NONE, StateChangeResult.NO_ERROR);
        assertThat(result).isEqualTo(AcInstanceStateResolver.UNLOCK);

        // lock
        result = acTypeStateResolver.resolve(DeployOrder.NONE, LockOrder.LOCK, SubOrder.NONE,
            DeployState.DEPLOYED, LockState.UNLOCKED, SubState.NONE, StateChangeResult.NO_ERROR);
        assertThat(result).isEqualTo(AcInstanceStateResolver.LOCK);

        // migrate
        result = acTypeStateResolver.resolve(DeployOrder.MIGRATE, LockOrder.NONE, SubOrder.NONE,
            DeployState.DEPLOYED, LockState.LOCKED, SubState.NONE, StateChangeResult.NO_ERROR);
        assertThat(result).isEqualTo(AcInstanceStateResolver.MIGRATE);

        // migrate-precheck
        result = acTypeStateResolver.resolve(DeployOrder.NONE, LockOrder.NONE, SubOrder.MIGRATE_PRECHECK,
            DeployState.DEPLOYED, LockState.LOCKED, SubState.NONE, StateChangeResult.NO_ERROR);
        assertThat(result).isEqualTo(AcInstanceStateResolver.MIGRATE_PRECHECK);

        // prepare
        result = acTypeStateResolver.resolve(DeployOrder.NONE, LockOrder.NONE, SubOrder.PREPARE,
            DeployState.UNDEPLOYED, LockState.NONE, SubState.NONE, StateChangeResult.NO_ERROR);
        assertThat(result).isEqualTo(AcInstanceStateResolver.PREPARE);

        // review
        result = acTypeStateResolver.resolve(DeployOrder.NONE, LockOrder.NONE, SubOrder.REVIEW,
            DeployState.DEPLOYED, LockState.LOCKED, SubState.NONE, StateChangeResult.NO_ERROR);
        assertThat(result).isEqualTo(AcInstanceStateResolver.REVIEW);

        // rollback
        result = acTypeStateResolver.resolve(DeployOrder.MIGRATION_REVERT, LockOrder.NONE, SubOrder.NONE,
                DeployState.MIGRATING, LockState.LOCKED, SubState.NONE, StateChangeResult.FAILED);
        assertThat(result).isEqualTo(AcInstanceStateResolver.MIGRATION_REVERT);

        result = acTypeStateResolver.resolve(DeployOrder.UNDEPLOY, LockOrder.NONE, SubOrder.NONE,
            DeployState.MIGRATION_REVERTING, LockState.LOCKED, SubState.NONE, StateChangeResult.FAILED);
        assertThat(result).isEqualTo(AcInstanceStateResolver.UNDEPLOY);

    }

    @Test
    void testResolveWrongOrder() {
        var acTypeStateResolver = new AcInstanceStateResolver();

        var result = acTypeStateResolver.resolve(DeployOrder.NONE, LockOrder.NONE, SubOrder.NONE,
            DeployState.UNDEPLOYED, LockState.NONE, SubState.NONE, StateChangeResult.NO_ERROR);
        assertThat(result).isEqualTo(AcInstanceStateResolver.NONE);

        result = acTypeStateResolver.resolve(DeployOrder.UNDEPLOY, LockOrder.UNLOCK, SubOrder.NONE,
            DeployState.DEPLOYED, LockState.LOCKED, SubState.NONE, StateChangeResult.NO_ERROR);
        assertThat(result).isEqualTo(AcInstanceStateResolver.NONE);

        result = acTypeStateResolver.resolve(DeployOrder.NONE, LockOrder.UNLOCK, SubOrder.NONE,
            DeployState.UNDEPLOYED, LockState.NONE, SubState.NONE, StateChangeResult.NO_ERROR);
        assertThat(result).isEqualTo(AcInstanceStateResolver.NONE);

        result = acTypeStateResolver.resolve(DeployOrder.UNDEPLOY, LockOrder.NONE, SubOrder.NONE,
            DeployState.DEPLOYING, LockState.NONE, SubState.NONE, StateChangeResult.NO_ERROR);
        assertThat(result).isEqualTo(AcInstanceStateResolver.NONE);

        result = acTypeStateResolver.resolve(null, null, null, null, null, null, null);
        assertThat(result).isEqualTo(AcInstanceStateResolver.NONE);
    }
}

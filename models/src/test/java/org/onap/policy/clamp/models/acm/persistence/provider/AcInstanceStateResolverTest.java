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

package org.onap.policy.clamp.models.acm.persistence.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;

class AcInstanceStateResolverTest {

    @Test
    void testResolve() {
        var acTypeStateResolver = new AcInstanceStateResolver();
        var result =
                acTypeStateResolver.resolve(DeployOrder.DEPLOY, LockOrder.NONE, DeployState.UNDEPLOYED, LockState.NONE);
        assertThat(result).isEqualTo(AcInstanceStateResolver.DEPLOY);
        result = acTypeStateResolver.resolve(DeployOrder.UNDEPLOY, LockOrder.NONE, DeployState.DEPLOYED,
                LockState.LOCKED);
        assertThat(result).isEqualTo(AcInstanceStateResolver.UNDEPLOY);
        result = acTypeStateResolver.resolve(DeployOrder.NONE, LockOrder.UNLOCK, DeployState.DEPLOYED,
                LockState.LOCKED);
        assertThat(result).isEqualTo(AcInstanceStateResolver.UNLOCK);
        result = acTypeStateResolver.resolve(DeployOrder.NONE, LockOrder.LOCK, DeployState.DEPLOYED,
                LockState.UNLOCKED);
        assertThat(result).isEqualTo(AcInstanceStateResolver.LOCK);

        result = acTypeStateResolver.resolve(DeployOrder.NONE, LockOrder.NONE, DeployState.UNDEPLOYED, LockState.NONE);
        assertThat(result).isEqualTo(AcInstanceStateResolver.NONE);
        result = acTypeStateResolver.resolve(DeployOrder.UNDEPLOY, LockOrder.UNLOCK, DeployState.DEPLOYED,
                LockState.LOCKED);
        assertThat(result).isEqualTo(AcInstanceStateResolver.NONE);
        result = acTypeStateResolver.resolve(DeployOrder.NONE, LockOrder.UNLOCK, DeployState.UNDEPLOYED,
                LockState.NONE);
        assertThat(result).isEqualTo(AcInstanceStateResolver.NONE);
        result = acTypeStateResolver.resolve(DeployOrder.UNDEPLOY, LockOrder.NONE, DeployState.DEPLOYING,
                LockState.NONE);
        assertThat(result).isEqualTo(AcInstanceStateResolver.NONE);

        result = acTypeStateResolver.resolve(null, null, null, null);
        assertThat(result).isEqualTo(AcInstanceStateResolver.NONE);
    }

}

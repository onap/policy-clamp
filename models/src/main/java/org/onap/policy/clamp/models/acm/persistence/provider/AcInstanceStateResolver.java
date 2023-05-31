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

import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ErrorStatus;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.clamp.models.acm.utils.StateDefinition;
import org.springframework.stereotype.Component;

@Component
public class AcInstanceStateResolver {
    private final StateDefinition<String> graph;

    private static final String DEPLOYED = DeployState.DEPLOYED.name();
    private static final String DEPLOYING = DeployState.DEPLOYING.name();
    private static final String UNDEPLOYED = DeployState.UNDEPLOYED.name();
    private static final String UNDEPLOYING = DeployState.UNDEPLOYING.name();
    private static final String UPDATING = DeployState.UPDATING.name();
    private static final String DELETING = DeployState.DELETING.name();

    private static final String LOCKED = LockState.LOCKED.name();
    private static final String LOCKING = LockState.LOCKING.name();
    private static final String UNLOCKED = LockState.UNLOCKED.name();
    private static final String UNLOCKING = LockState.UNLOCKING.name();
    private static final String STATE_LOCKED_NONE = LockState.NONE.name();

    private static final String DEPLOY_NONE = DeployOrder.NONE.name();
    private static final String LOCK_NONE = LockOrder.NONE.name();

    private static final String NO_ERROR = ErrorStatus.NO_ERROR.name();
    private static final String FAILED = ErrorStatus.FAILED.name();

    // list of results
    public static final String DEPLOY = DeployOrder.DEPLOY.name();
    public static final String UNDEPLOY = DeployOrder.UNDEPLOY.name();
    public static final String DELETE = DeployOrder.DELETE.name();
    public static final String LOCK = LockOrder.LOCK.name();
    public static final String UNLOCK = LockOrder.UNLOCK.name();
    public static final String NONE = "NONE";

    /**
     * Construct.
     */
    public AcInstanceStateResolver() {
        this.graph = new StateDefinition<>(5, NONE);

        // no error
        this.graph.put(new String[] {DEPLOY, LOCK_NONE, UNDEPLOYED, STATE_LOCKED_NONE, NO_ERROR}, DEPLOY);
        this.graph.put(new String[] {UNDEPLOY, LOCK_NONE, DEPLOYED, LOCKED, NO_ERROR}, UNDEPLOY);
        this.graph.put(new String[] {DELETE, LOCK_NONE, UNDEPLOYED, LOCK_NONE, NO_ERROR}, DELETE);
        this.graph.put(new String[] {DEPLOY_NONE, UNLOCK, DEPLOYED, LOCKED, NO_ERROR}, UNLOCK);
        this.graph.put(new String[] {DEPLOY_NONE, LOCK, DEPLOYED, UNLOCKED, NO_ERROR}, LOCK);

        // failed
        this.graph.put(new String[] {DEPLOY, LOCK_NONE, UNDEPLOYING, STATE_LOCKED_NONE, FAILED}, DEPLOY);
        this.graph.put(new String[] {UNDEPLOY, LOCK_NONE, UNDEPLOYING, STATE_LOCKED_NONE, FAILED}, UNDEPLOY);
        this.graph.put(new String[] {UNDEPLOY, LOCK_NONE, UPDATING, STATE_LOCKED_NONE, FAILED}, UNDEPLOY);

        this.graph.put(new String[] {DEPLOY, LOCK_NONE, DEPLOYING, STATE_LOCKED_NONE, FAILED}, DEPLOY);
        this.graph.put(new String[] {UNDEPLOY, LOCK_NONE, DEPLOYING, STATE_LOCKED_NONE, FAILED}, UNDEPLOY);

        this.graph.put(new String[] {DELETE, LOCK_NONE, DELETING, LOCK_NONE, FAILED}, DELETE);

        this.graph.put(new String[] {DEPLOY_NONE, UNLOCK, DEPLOYED, LOCKING, FAILED}, UNLOCK);
        this.graph.put(new String[] {DEPLOY_NONE, LOCK, DEPLOYED, LOCKING, FAILED}, LOCK);

        this.graph.put(new String[] {DEPLOY_NONE, LOCK, DEPLOYED, UNLOCKING, FAILED}, LOCK);
        this.graph.put(new String[] {DEPLOY_NONE, UNLOCK, DEPLOYED, UNLOCKING, FAILED}, UNLOCK);
    }

    /**
     * Check if Deploy Order and Lock Order are consistent with current DeployState and LockState.
     *
     * @param acDeployOrder the Deploy Ordered
     * @param acLockOrder the Lock Ordered
     * @param acDeployState then current Deploy State
     * @param acLockState the current Lock State
     * @param acErrorStatus the current Error Status
     * @return the order (DEPLOY/UNDEPLOY/LOCK/UNLOCK) to send to participant or NONE if order is not consistent
     */
    public String resolve(DeployOrder acDeployOrder, LockOrder acLockOrder, DeployState acDeployState,
            LockState acLockState, ErrorStatus acErrorStatus) {
        var deployOrder = acDeployOrder != null ? acDeployOrder : DeployOrder.NONE;
        var lockOrder = acLockOrder != null ? acLockOrder : LockOrder.NONE;
        var errorStatus = acErrorStatus != null ? acErrorStatus : ErrorStatus.NO_ERROR;

        var deployState = acDeployState != null ? acDeployState : DeployState.UNDEPLOYED;
        var lockState = acLockState != null ? acLockState : LockState.NONE;
        return this.graph.get(new String[] {deployOrder.name(), lockOrder.name(), deployState.name(), lockState.name(),
                errorStatus.name()});
    }
}

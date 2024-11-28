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

package org.onap.policy.clamp.models.acm.persistence.provider;

import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.SubOrder;
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
    private static final String MIGRATING = DeployState.MIGRATING.name();
    private static final String MIGRATION_PRECHECKING = SubState.MIGRATION_PRECHECKING.name();
    private static final String SUB_STATE_NONE = SubState.NONE.name();

    private static final String LOCKED = LockState.LOCKED.name();
    private static final String LOCKING = LockState.LOCKING.name();
    private static final String UNLOCKED = LockState.UNLOCKED.name();
    private static final String UNLOCKING = LockState.UNLOCKING.name();
    private static final String STATE_LOCKED_NONE = LockState.NONE.name();

    private static final String DEPLOY_NONE = DeployOrder.NONE.name();
    private static final String LOCK_NONE = LockOrder.NONE.name();
    private static final String SUB_NONE = SubOrder.NONE.name();

    private static final String NO_ERROR = StateChangeResult.NO_ERROR.name();
    private static final String FAILED = StateChangeResult.FAILED.name();
    private static final String TIMEOUT = StateChangeResult.TIMEOUT.name();

    // list of results
    public static final String DEPLOY = DeployOrder.DEPLOY.name();
    public static final String UNDEPLOY = DeployOrder.UNDEPLOY.name();
    public static final String DELETE = DeployOrder.DELETE.name();
    public static final String LOCK = LockOrder.LOCK.name();
    public static final String UNLOCK = LockOrder.UNLOCK.name();
    public static final String MIGRATE = DeployOrder.MIGRATE.name();
    public static final String MIGRATE_PRECHECK = SubOrder.MIGRATE_PRECHECK.name();
    public static final String PREPARE = SubOrder.PREPARE.name();
    public static final String REVIEW = SubOrder.REVIEW.name();
    public static final String UPDATE = DeployOrder.UPDATE.name();
    public static final String NONE = "NONE";

    /**
     * Construct.
     */
    public AcInstanceStateResolver() {
        this.graph = new StateDefinition<>(7, NONE);

        // make an order when there are no fails
        this.graph.put(new String[] {DEPLOY, LOCK_NONE, SUB_NONE,
            UNDEPLOYED, STATE_LOCKED_NONE, SUB_STATE_NONE, NO_ERROR}, DEPLOY);
        this.graph.put(new String[] {UNDEPLOY, LOCK_NONE, SUB_NONE,
            DEPLOYED, LOCKED, SUB_STATE_NONE, NO_ERROR}, UNDEPLOY);
        this.graph.put(new String[] {DELETE, LOCK_NONE, SUB_NONE,
            UNDEPLOYED, LOCK_NONE, SUB_STATE_NONE, NO_ERROR}, DELETE);
        this.graph.put(new String[] {DEPLOY_NONE, UNLOCK, SUB_NONE,
            DEPLOYED, LOCKED, SUB_STATE_NONE, NO_ERROR}, UNLOCK);
        this.graph.put(new String[] {DEPLOY_NONE, LOCK, SUB_NONE,
            DEPLOYED, UNLOCKED, SUB_STATE_NONE, NO_ERROR}, LOCK);
        this.graph.put(new String[] {MIGRATE, LOCK_NONE, SUB_NONE,
            DEPLOYED, LOCKED, SUB_STATE_NONE, NO_ERROR}, MIGRATE);
        this.graph.put(new String[] {UPDATE, LOCK_NONE, SUB_NONE,
            DEPLOYED, LOCKED, SUB_STATE_NONE, NO_ERROR}, UPDATE);
        this.graph.put(new String[] {DEPLOY_NONE, LOCK_NONE, REVIEW,
            DEPLOYED, LOCKED, SUB_STATE_NONE, NO_ERROR}, REVIEW);
        this.graph.put(new String[] {DEPLOY_NONE, LOCK_NONE, PREPARE,
            UNDEPLOYED, STATE_LOCKED_NONE, SUB_STATE_NONE, NO_ERROR}, PREPARE);
        this.graph.put(new String[] {DEPLOY_NONE, LOCK_NONE, MIGRATE_PRECHECK,
            DEPLOYED, LOCKED, SUB_STATE_NONE, NO_ERROR}, MIGRATE_PRECHECK);

        // make an order in a failed scenario
        setAllowed(DEPLOY);
        setAllowed(UNDEPLOY);
        this.graph.put(new String[] {UNDEPLOY, LOCK_NONE, SUB_NONE,
            UPDATING, LOCKED, SUB_STATE_NONE, FAILED}, UNDEPLOY);
        this.graph.put(new String[] {UNDEPLOY, LOCK_NONE, SUB_NONE,
            MIGRATING, LOCKED, SUB_STATE_NONE, FAILED}, UNDEPLOY);

        this.graph.put(new String[] {DELETE, LOCK_NONE, SUB_NONE,
            DELETING, LOCK_NONE, SUB_STATE_NONE, FAILED}, DELETE);

        this.graph.put(new String[] {DEPLOY_NONE, UNLOCK, SUB_NONE,
            DEPLOYED, LOCKING, SUB_STATE_NONE, FAILED}, UNLOCK);
        this.graph.put(new String[] {DEPLOY_NONE, UNLOCK, SUB_NONE,
            DEPLOYED, UNLOCKING, SUB_STATE_NONE, FAILED}, UNLOCK);

        this.graph.put(new String[] {DEPLOY_NONE, LOCK, SUB_NONE, DEPLOYED, LOCKING, SUB_STATE_NONE, FAILED}, LOCK);
        this.graph.put(new String[] {DEPLOY_NONE, LOCK, SUB_NONE, DEPLOYED, UNLOCKING, SUB_STATE_NONE, FAILED}, LOCK);

        this.graph.put(new String[] {UPDATE, LOCK_NONE, SUB_NONE, UPDATING, LOCKED, SUB_STATE_NONE, FAILED}, UPDATE);

        this.graph.put(new String[] {DEPLOY_NONE, LOCK_NONE, MIGRATE_PRECHECK,
            DEPLOYED, LOCKED, MIGRATION_PRECHECKING, FAILED}, MIGRATE_PRECHECK);

        // timeout
        this.graph.put(new String[] {UNDEPLOY, LOCK_NONE, SUB_NONE,
            UPDATING, LOCKED, SUB_STATE_NONE, TIMEOUT}, UNDEPLOY);
        this.graph.put(new String[] {UNDEPLOY, LOCK_NONE, SUB_NONE,
            MIGRATING, LOCKED, SUB_STATE_NONE, TIMEOUT}, UNDEPLOY);
        this.graph.put(new String[] {UNDEPLOY, LOCK_NONE, SUB_NONE,
            MIGRATION_PRECHECKING, LOCKED, SUB_STATE_NONE, TIMEOUT}, UNDEPLOY);

        this.graph.put(new String[] {DELETE, LOCK_NONE, SUB_NONE,
            DELETING, LOCK_NONE, SUB_STATE_NONE, TIMEOUT}, DELETE);

        this.graph.put(new String[] {DEPLOY_NONE, UNLOCK, SUB_NONE,
            DEPLOYED, LOCKING, SUB_STATE_NONE, TIMEOUT}, UNLOCK);
        this.graph.put(new String[] {DEPLOY_NONE, LOCK, SUB_NONE,
            DEPLOYED, LOCKING, SUB_STATE_NONE, TIMEOUT}, LOCK);

        this.graph.put(new String[] {DEPLOY_NONE, LOCK, SUB_NONE,
            DEPLOYED, UNLOCKING, SUB_STATE_NONE, TIMEOUT}, LOCK);
        this.graph.put(new String[] {DEPLOY_NONE, UNLOCK, SUB_NONE,
            DEPLOYED, UNLOCKING, SUB_STATE_NONE, TIMEOUT}, UNLOCK);

        this.graph.put(new String[] {UPDATE, LOCK_NONE, SUB_NONE, UPDATING, LOCKED, SUB_STATE_NONE, TIMEOUT}, UPDATE);

        this.graph.put(new String[] {DEPLOY_NONE, LOCK_NONE, MIGRATE_PRECHECK,
            DEPLOYED, LOCKED, MIGRATION_PRECHECKING, TIMEOUT}, MIGRATE_PRECHECK);
    }

    private void setAllowed(String deployOrder) {
        this.graph.put(new String[] {deployOrder, LOCK_NONE, SUB_NONE,
            UNDEPLOYING, STATE_LOCKED_NONE, SUB_STATE_NONE, FAILED}, deployOrder);
        this.graph.put(new String[] {deployOrder, LOCK_NONE, SUB_NONE,
            UNDEPLOYING, LOCKED, SUB_STATE_NONE, FAILED}, deployOrder);

        this.graph.put(new String[] {deployOrder, LOCK_NONE, SUB_NONE,
            UNDEPLOYING, STATE_LOCKED_NONE, SUB_STATE_NONE, TIMEOUT}, deployOrder);
        this.graph.put(new String[] {deployOrder, LOCK_NONE, SUB_NONE,
            UNDEPLOYING, LOCKED, SUB_STATE_NONE, TIMEOUT}, deployOrder);

        this.graph.put(new String[] {deployOrder, LOCK_NONE, SUB_NONE,
            DEPLOYING, STATE_LOCKED_NONE, SUB_STATE_NONE, FAILED}, deployOrder);
        this.graph.put(new String[] {deployOrder, LOCK_NONE, SUB_NONE,
            DEPLOYING, LOCKED, SUB_STATE_NONE, FAILED}, deployOrder);

        this.graph.put(new String[] {deployOrder, LOCK_NONE, SUB_NONE,
            DEPLOYING, STATE_LOCKED_NONE, SUB_STATE_NONE, TIMEOUT}, deployOrder);
        this.graph.put(new String[] {deployOrder, LOCK_NONE, SUB_NONE,
            DEPLOYING, LOCKED, SUB_STATE_NONE, TIMEOUT}, deployOrder);
    }

    /**
     * Check if Deploy Order and Lock Order are consistent with current DeployState and LockState.
     *
     * @param acDeployOrder the Deploy Ordered
     * @param acLockOrder the Lock Ordered
     * @param acSubOrder the Sub Ordered
     * @param acDeployState then current Deploy State
     * @param acLockState the current Lock State
     * @param acSubState the current Sub State
     * @param acStateChangeResult the current Result of the State Change
     * @return the order (DEPLOY/UNDEPLOY/LOCK/UNLOCK) to send to participant or NONE if order is not consistent
     */
    public String resolve(DeployOrder acDeployOrder, LockOrder acLockOrder, SubOrder acSubOrder,
        DeployState acDeployState, LockState acLockState, SubState acSubState, StateChangeResult acStateChangeResult) {
        var deployOrder = acDeployOrder != null ? acDeployOrder : DeployOrder.NONE;
        var lockOrder = acLockOrder != null ? acLockOrder : LockOrder.NONE;
        var subOrder = acSubOrder != null ? acSubOrder : SubOrder.NONE;
        var stateChangeResult = acStateChangeResult != null ? acStateChangeResult : StateChangeResult.NO_ERROR;

        var deployState = acDeployState != null ? acDeployState : DeployState.UNDEPLOYED;
        var lockState = acLockState != null ? acLockState : LockState.NONE;
        var subState = acSubState != null ? acSubState : SubState.NONE;
        return this.graph.get(new String[] {deployOrder.name(), lockOrder.name(), subOrder.name(),
                deployState.name(), lockState.name(), subState.name(), stateChangeResult.name()});
    }
}

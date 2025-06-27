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

    // DeployState
    private static final String DEPLOYED = DeployState.DEPLOYED.name();
    private static final String DEPLOYING = DeployState.DEPLOYING.name();
    private static final String UNDEPLOYED = DeployState.UNDEPLOYED.name();
    private static final String UNDEPLOYING = DeployState.UNDEPLOYING.name();
    private static final String UPDATING = DeployState.UPDATING.name();
    private static final String DELETING = DeployState.DELETING.name();
    private static final String MIGRATING = DeployState.MIGRATING.name();
    private static final String MIGRATION_REVERTING = DeployState.MIGRATION_REVERTING.name();

    // SubState
    private static final String MIGRATION_PRECHECKING = SubState.MIGRATION_PRECHECKING.name();
    private static final String SUB_STATE_NONE = SubState.NONE.name();
    private static final String PREPARING = SubState.PREPARING.name();
    private static final String REVIEWING = SubState.REVIEWING.name();

    // LockState
    private static final String LOCKED = LockState.LOCKED.name();
    private static final String LOCKING = LockState.LOCKING.name();
    private static final String UNLOCKED = LockState.UNLOCKED.name();
    private static final String UNLOCKING = LockState.UNLOCKING.name();
    private static final String STATE_LOCKED_NONE = LockState.NONE.name();

    // NONE Order
    private static final String DEPLOY_NONE = DeployOrder.NONE.name();
    private static final String LOCK_NONE = LockOrder.NONE.name();
    private static final String SUB_NONE = SubOrder.NONE.name();

    // DeployOrder
    public static final String DEPLOY = DeployOrder.DEPLOY.name();
    public static final String UNDEPLOY = DeployOrder.UNDEPLOY.name();
    public static final String DELETE = DeployOrder.DELETE.name();
    public static final String MIGRATE = DeployOrder.MIGRATE.name();
    public static final String MIGRATION_REVERT = DeployOrder.MIGRATION_REVERT.name();
    public static final String UPDATE = DeployOrder.UPDATE.name();

    // LockOrder
    public static final String LOCK = LockOrder.LOCK.name();
    public static final String UNLOCK = LockOrder.UNLOCK.name();

    // SubOrder
    public static final String MIGRATE_PRECHECK = SubOrder.MIGRATE_PRECHECK.name();
    public static final String PREPARE = SubOrder.PREPARE.name();
    public static final String REVIEW = SubOrder.REVIEW.name();

    // StateChangeResult
    private static final String NO_ERROR = StateChangeResult.NO_ERROR.name();
    private static final String FAILED = StateChangeResult.FAILED.name();
    private static final String TIMEOUT = StateChangeResult.TIMEOUT.name();

    public static final String NONE = "NONE";

    /**
     * Construct.
     */
    public AcInstanceStateResolver() {
        this.graph = new StateDefinition<>(7, NONE);

        // make an order when there are no fails
        addDeployOrder(DEPLOY, UNDEPLOYED, STATE_LOCKED_NONE);
        addDeployOrder(UNDEPLOY, DEPLOYED, LOCKED);
        addDeployOrder(DELETE, UNDEPLOYED, STATE_LOCKED_NONE);
        addDeployOrder(MIGRATE, DEPLOYED, LOCKED);
        addDeployOrder(UPDATE, DEPLOYED, LOCKED);

        addLockOrder(UNLOCK, LOCKED);
        addLockOrder(LOCK, UNLOCKED);

        addSubOrder(REVIEW, DEPLOYED, LOCKED);
        addSubOrder(PREPARE, UNDEPLOYED, STATE_LOCKED_NONE);
        addSubOrder(MIGRATE_PRECHECK, DEPLOYED, LOCKED);

        // failed or timeout scenario
        setAllowed(DEPLOY);
        setAllowed(UNDEPLOY);

        // undeploy order in a failed or timeout scenario
        addDeployOrderWithFail(UNDEPLOY, UPDATING, LOCKED, SUB_STATE_NONE);
        addDeployOrderWithFail(UNDEPLOY, MIGRATING, LOCKED, SUB_STATE_NONE);
        addDeployOrderWithFail(UNDEPLOY, MIGRATION_PRECHECKING, LOCKED, SUB_STATE_NONE);
        addDeployOrderWithFail(UNDEPLOY, REVIEWING, LOCKED, SUB_STATE_NONE);

        // delete order in a failed or timeout scenario
        addDeployOrderWithFail(DELETE, DELETING, LOCK_NONE, SUB_STATE_NONE);
        addDeployOrderWithFail(DELETE, UNDEPLOYED, LOCK_NONE, PREPARING);

        // update order in a failed or timeout scenario
        addDeployOrderWithFail(UPDATE, UPDATING, LOCKED, SUB_STATE_NONE);

        // unlock order in a failed or timeout scenario
        addLockOrderWithFail(UNLOCK, LOCKING);
        addLockOrderWithFail(UNLOCK, UNLOCKING);

        // lock order in a failed or timeout scenario
        addLockOrderWithFail(LOCK, LOCKING);
        addLockOrderWithFail(LOCK, UNLOCKING);

        // migrate-precheck order in a failed or timeout scenario
        addSubOrderWithFail(MIGRATE_PRECHECK, DEPLOYED, LOCKED, MIGRATION_PRECHECKING);

        // prepare order in a failed or timeout scenario
        addSubOrderWithFail(PREPARE, UNDEPLOYED, LOCK_NONE, PREPARING);

        // review order in a failed or timeout scenario
        addSubOrderWithFail(REVIEW, DEPLOYED, LOCKED, REVIEWING);

        // rollback
        addDeployOrderWithFail(MIGRATION_REVERT, MIGRATING, LOCKED, SUB_STATE_NONE);
        addDeployOrderWithFail(UNDEPLOY, MIGRATION_REVERTING, LOCKED, SUB_STATE_NONE);
    }

    private void addDeployOrder(String deployOrder, String deployState, String lockState) {
        this.graph.put(new String[] {
            deployOrder, LOCK_NONE, SUB_NONE, deployState, lockState, STATE_LOCKED_NONE, NO_ERROR}, deployOrder);
    }

    private void addDeployOrderWithFail(String deployOrder, String deployState, String lockState, String subState) {
        this.graph.put(new String[] {
            deployOrder, LOCK_NONE, SUB_NONE, deployState, lockState, subState, FAILED}, deployOrder);
        this.graph.put(new String[] {
            deployOrder, LOCK_NONE, SUB_NONE, deployState, lockState, subState, TIMEOUT}, deployOrder);
    }

    private void addSubOrder(String subOrder, String deployState, String lockState) {
        this.graph.put(new String[] {
            DEPLOY_NONE, LOCK_NONE, subOrder, deployState, lockState, SUB_STATE_NONE, NO_ERROR}, subOrder);
    }

    private void addSubOrderWithFail(String subOrder, String deployState, String lockState, String subState) {
        this.graph.put(new String[] {
            DEPLOY_NONE, LOCK_NONE, subOrder, deployState, lockState, subState, FAILED}, subOrder);
        this.graph.put(new String[] {
            DEPLOY_NONE, LOCK_NONE, subOrder, deployState, lockState, subState, TIMEOUT}, subOrder);
    }

    private void addLockOrder(String lockOrder, String lockState) {
        this.graph.put(new String[] {
            DEPLOY_NONE, lockOrder, SUB_NONE, DEPLOYED, lockState, SUB_STATE_NONE, NO_ERROR}, lockOrder);
    }

    private void addLockOrderWithFail(String lockOrder, String lockState) {
        this.graph.put(new String[] {
            DEPLOY_NONE, lockOrder, SUB_NONE, DEPLOYED, lockState, SUB_STATE_NONE, FAILED}, lockOrder);
        this.graph.put(new String[] {
            DEPLOY_NONE, lockOrder, SUB_NONE, DEPLOYED, lockState, SUB_STATE_NONE, TIMEOUT}, lockOrder);
    }

    private void setAllowed(String deployOrder) {
        addDeployOrderWithFail(deployOrder, UNDEPLOYING, STATE_LOCKED_NONE, SUB_STATE_NONE);
        addDeployOrderWithFail(deployOrder, UNDEPLOYING, LOCKED, SUB_STATE_NONE);
        addDeployOrderWithFail(deployOrder, DEPLOYING, STATE_LOCKED_NONE, SUB_STATE_NONE);
        addDeployOrderWithFail(deployOrder, DEPLOYING, LOCKED, SUB_STATE_NONE);
    }

    /**
     * Check if Deploy Order and Lock Order are consistent with current DeployState and LockState.
     *
     * @param acDeployOrder the Deploy Ordered
     * @param acLockOrder the Lock Ordered
     * @param acSubOrder the Sub Ordered
     * @param acDeployState the current Deploy State
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

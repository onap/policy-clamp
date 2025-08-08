/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022,2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.persistence.repository;

import java.util.Collection;
import java.util.List;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationComposition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AutomationCompositionRepository extends JpaRepository<JpaAutomationComposition, String> {

    List<JpaAutomationComposition> findByCompositionId(String compositionId);

    List<JpaAutomationComposition> findByDeployStateIn(Collection<DeployState> deployStates);

    Page<JpaAutomationComposition> findByDeployStateIn(Collection<DeployState> deployStates,
                                                       Pageable pageable);

    List<JpaAutomationComposition> findByLockStateIn(Collection<LockState> lockStates);

    List<JpaAutomationComposition> findBySubStateIn(Collection<SubState> subStates);

    Page<JpaAutomationComposition> findByStateChangeResultInAndDeployStateIn(
            Collection<StateChangeResult> stateChangeResults, Collection<DeployState> deployStates,
            Pageable pageable);

    Page<JpaAutomationComposition> findByStateChangeResultIn(Collection<StateChangeResult> stateChangeResults,
                                                             Pageable pageable);

    Page<JpaAutomationComposition> findByCompositionIdIn(Collection<String> compositionIds, Pageable pageable);

    Page<JpaAutomationComposition> findByCompositionIdInAndStateChangeResultIn(
        Collection<String> compositionIds, Collection<StateChangeResult> stateChangeResults, Pageable pageable);

    Page<JpaAutomationComposition> findByCompositionIdInAndDeployStateIn(
        Collection<String> compositionIds, Collection<DeployState> deployStates, Pageable pageable);

    Page<JpaAutomationComposition> findByCompositionIdInAndStateChangeResultInAndDeployStateIn(
        Collection<String> compositionIds, Collection<StateChangeResult> stateChangeResults,
        Collection<DeployState> deployStates, Pageable pageable);
}

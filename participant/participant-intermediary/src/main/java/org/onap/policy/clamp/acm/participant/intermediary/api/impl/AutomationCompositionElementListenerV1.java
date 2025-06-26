/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.api.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.models.base.PfModelException;

public interface AutomationCompositionElementListenerV1 extends AutomationCompositionElementListener {

    void undeploy(UUID automationCompositionId, UUID automationCompositionElementId) throws PfModelException;

    void deploy(UUID automationCompositionId, AcElementDeploy element, Map<String, Object> properties)
            throws PfModelException;

    void lock(UUID automationCompositionId, UUID automationCompositionElementId) throws PfModelException;

    void unlock(UUID automationCompositionId, UUID automationCompositionElementId) throws PfModelException;

    void delete(UUID automationCompositionId, UUID automationCompositionElementId) throws PfModelException;

    void update(UUID automationCompositionId, AcElementDeploy element, Map<String, Object> properties)
            throws PfModelException;

    void prime(UUID compositionId, List<AutomationCompositionElementDefinition> elementDefinitionList)
            throws PfModelException;

    void deprime(UUID compositionId) throws PfModelException;

    void migrate(UUID instanceId, AcElementDeploy element, UUID compositionTargetId,
            Map<String, Object> properties) throws PfModelException;
}

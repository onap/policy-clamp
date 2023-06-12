/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.a1pms.handler;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.validation.Validation;
import javax.validation.ValidationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.onap.policy.clamp.acm.participant.a1pms.exception.A1PolicyServiceException;
import org.onap.policy.clamp.acm.participant.a1pms.models.ConfigurationEntity;
import org.onap.policy.clamp.acm.participant.a1pms.webclient.AcA1PmsClient;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of automationCompositionElement updates.
 */
@Component
@RequiredArgsConstructor
public class AutomationCompositionElementHandler implements AutomationCompositionElementListener {

    private static final Coder CODER = new StandardCoder();

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ParticipantIntermediaryApi intermediaryApi;

    private final AcA1PmsClient acA1PmsClient;

    // Map of acElement Id and A1PMS services
    @Getter(AccessLevel.PACKAGE)
    private final Map<UUID, ConfigurationEntity> configRequestMap = new HashMap<>();

    /**
     * Handle a automation composition element state change.
     *
     * @param automationCompositionId the ID of the automation composition
     * @param automationCompositionElementId the ID of the automation composition element
     * @throws PfModelException in case of a model exception
     */
    @Override
    public void undeploy(UUID automationCompositionId, UUID automationCompositionElementId)
            throws A1PolicyServiceException {
        var configurationEntity = configRequestMap.get(automationCompositionElementId);
        if (configurationEntity != null && acA1PmsClient.isPmsHealthy()) {
            acA1PmsClient.deleteService(configurationEntity.getPolicyServiceEntities());
            configRequestMap.remove(automationCompositionElementId);
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                    "Undeployed");
        } else {
            LOGGER.warn("Failed to connect with A1PMS. Service configuration is: {}", configurationEntity);
            throw new A1PolicyServiceException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Unable to connect with A1PMS");
        }
    }

    /**
     * Callback method to handle an update on an automation composition element.
     *
     * @param automationCompositionId the ID of the automation composition
     * @param element the information on the automation composition element
     * @param properties properties Map
     */
    @Override
    public void deploy(UUID automationCompositionId, AcElementDeploy element, Map<String, Object> properties)
            throws A1PolicyServiceException {
        try {
            var configurationEntity = CODER.convert(properties, ConfigurationEntity.class);
            var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(configurationEntity);
            if (violations.isEmpty()) {
                if (acA1PmsClient.isPmsHealthy()) {
                    acA1PmsClient.createService(configurationEntity.getPolicyServiceEntities());
                    configRequestMap.put(element.getId(), configurationEntity);

                    intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                            DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");
                } else {
                    LOGGER.error("Failed to connect with A1PMS");
                    throw new A1PolicyServiceException(HttpStatus.SC_SERVICE_UNAVAILABLE,
                            "Unable to connect with A1PMS");
                }
            } else {
                LOGGER.error("Violations found in the config request parameters: {}", violations);
                throw new ValidationException("Constraint violations in the config request");
            }
        } catch (ValidationException | CoderException | A1PolicyServiceException e) {
            throw new A1PolicyServiceException(HttpStatus.SC_BAD_REQUEST, "Invalid Configuration", e);
        }
    }

    @Override
    public void lock(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.LOCKED,
                StateChangeResult.NO_ERROR, "Locked");
    }

    @Override
    public void unlock(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.UNLOCKED,
                StateChangeResult.NO_ERROR, "Unlocked");
    }

    @Override
    public void delete(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, DeployState.DELETED, null,
                StateChangeResult.NO_ERROR, "Deleted");
    }

    @Override
    public void update(UUID instanceId, AcElementDeploy element, Map<String, Object> properties)
            throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, element.getId(), DeployState.DEPLOYED, null,
                StateChangeResult.NO_ERROR, "Update not supported");
    }

    @Override
    public void prime(UUID compositionId, List<AutomationCompositionElementDefinition> elementDefinitionList)
            throws PfModelException {
        intermediaryApi.updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.NO_ERROR, "Primed");
    }

    @Override
    public void deprime(UUID compositionId) throws PfModelException {
        intermediaryApi.updateCompositionState(compositionId, AcTypeState.COMMISSIONED, StateChangeResult.NO_ERROR,
                "Deprimed");
    }
}

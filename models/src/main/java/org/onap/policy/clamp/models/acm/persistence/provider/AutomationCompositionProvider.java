/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2024 Nordix Foundation.
 * ================================================================================
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationComposition;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionElement;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionElementRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionRepository;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class provides information on automation composition concepts in the database to callers.
 */
@Service
@Transactional
@AllArgsConstructor
public class AutomationCompositionProvider {

    private final AutomationCompositionRepository automationCompositionRepository;
    private final AutomationCompositionElementRepository acElementRepository;

    /**
     * Get automation composition.
     *
     * @param instanceId the ID of the automation composition to get
     * @return the automation composition found
     */
    @Transactional(readOnly = true)
    public AutomationComposition getAutomationComposition(final UUID instanceId) {
        var result = automationCompositionRepository.findById(instanceId.toString());
        if (result.isEmpty()) {
            throw new PfModelRuntimeException(Status.NOT_FOUND, "AutomationComposition not found");
        }
        return result.get().toAuthorative();
    }

    /**
     * Find automation composition.
     *
     * @param instanceId the ID of the automation composition to get
     * @return the automation composition found
     */
    @Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
    public Optional<AutomationComposition> findAutomationComposition(final UUID instanceId) {
        var result = automationCompositionRepository.findById(instanceId.toString());
        return result.stream().map(JpaAutomationComposition::toAuthorative).findFirst();
    }

    /**
     * Find automation composition by automationCompositionId.
     *
     * @param automationCompositionId the ID of the automation composition to get
     * @return the automation composition found
     */
    @Transactional(readOnly = true)
    public Optional<AutomationComposition> findAutomationComposition(
            final ToscaConceptIdentifier automationCompositionId) {
        return automationCompositionRepository
                .findOne(createExample(null, automationCompositionId.getName(), automationCompositionId.getVersion()))
                .map(JpaAutomationComposition::toAuthorative);
    }

    /**
     * Create automation composition.
     *
     * @param automationComposition the automation composition to create
     * @return the create automation composition
     */
    public AutomationComposition createAutomationComposition(final AutomationComposition automationComposition) {
        automationComposition.setInstanceId(UUID.randomUUID());
        AcmUtils.setCascadedState(automationComposition, DeployState.UNDEPLOYED, LockState.NONE);
        var result = automationCompositionRepository.save(ProviderUtils.getJpaAndValidate(automationComposition,
                JpaAutomationComposition::new, "automation composition"));

        // Return the saved automation composition
        return result.toAuthorative();
    }


    /**
     * Update automation composition state.
     *
     * @param acSource the automation composition to update
     * @return the updated automation composition
     */
    public AutomationComposition updateAcState(final AutomationComposition acSource) {
        var automationComposition = automationCompositionRepository
                .getReferenceById(acSource.getInstanceId().toString());
        automationComposition.fromAuthorativeBase(acSource);
        var result = automationCompositionRepository.save(automationComposition);
        automationCompositionRepository.flush();
        // Return the saved automation composition
        return result.toAuthorative();
    }

    /**
     * Update automation composition.
     *
     * @param automationComposition the automation composition to update
     * @return the updated automation composition
     */
    public AutomationComposition updateAutomationComposition(
            @NonNull final AutomationComposition automationComposition) {
        var result = automationCompositionRepository.save(ProviderUtils.getJpaAndValidate(automationComposition,
                JpaAutomationComposition::new, "automation composition"));
        automationCompositionRepository.flush();
        // Return the saved automation composition
        return result.toAuthorative();
    }

    /**
     * Get all automation compositions by compositionId.
     *
     * @param compositionId the compositionId of the automation composition definition
     * @return all automation compositions found
     */
    @Transactional(readOnly = true)
    public List<AutomationComposition> getAcInstancesByCompositionId(UUID compositionId) {
        return ProviderUtils
                .asEntityList(automationCompositionRepository.findByCompositionId(compositionId.toString()));
    }

    /**
     * Get all automation compositions in transition..
     *
     * @return all automation compositions found
     */
    @Transactional(readOnly = true)
    public List<AutomationComposition> getAcInstancesInTransition() {
        var jpaList = automationCompositionRepository.findByDeployStateIn(List.of(DeployState.DEPLOYING,
            DeployState.UNDEPLOYING, DeployState.DELETING, DeployState.UPDATING, DeployState.MIGRATING));
        jpaList.addAll(automationCompositionRepository.findByLockStateIn(
            List.of(LockState.LOCKING, LockState.UNLOCKING)));
        jpaList.addAll(automationCompositionRepository.findBySubStateIn(
                List.of(SubState.PREPARING, SubState.MIGRATION_PRECHECKING, SubState.REVIEWING)));
        return ProviderUtils.asEntityList(jpaList);
    }

    /**
     * Get automation compositions.
     *
     * @param name the name of the automation composition to get, null to get all automation compositions
     * @param version the version of the automation composition to get, null to get all automation compositions
     * @return the automation compositions found
     */
    @Transactional(readOnly = true)
    public List<AutomationComposition> getAutomationCompositions(final UUID compositionId, final String name,
            final String version) {

        return ProviderUtils
                .asEntityList(automationCompositionRepository.findAll(createExample(compositionId, name, version)));
    }

    private Example<JpaAutomationComposition> createExample(final UUID compositionId, final String name,
            final String version) {
        var example = new JpaAutomationComposition();
        example.setCompositionId(compositionId != null ? compositionId.toString() : null);
        example.setName(name);
        example.setVersion(version);
        example.setInstanceId(null);
        example.setElements(null);
        example.setDeployState(null);
        example.setLockState(null);

        return Example.of(example);
    }

    /**
     * Delete a automation composition.
     *
     * @param instanceId the ID of the automation composition to get
     * @return the automation composition deleted
     */
    public AutomationComposition deleteAutomationComposition(@NonNull final UUID instanceId) {
        var jpaDeleteAutomationComposition = automationCompositionRepository.findById(instanceId.toString());
        if (jpaDeleteAutomationComposition.isEmpty()) {
            var errorMessage = "delete of automation composition \"" + instanceId
                    + "\" failed, automation composition does not exist";
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND, errorMessage);
        }

        automationCompositionRepository.deleteById(instanceId.toString());

        return jpaDeleteAutomationComposition.get().toAuthorative();
    }

    /**
     * Upgrade States.
     *
     * @param automationCompositionInfoList list of AutomationCompositionInfo
     */
    public void upgradeStates(@NonNull final List<AutomationCompositionInfo> automationCompositionInfoList) {
        if (automationCompositionInfoList.isEmpty()) {
            return;
        }
        List<JpaAutomationCompositionElement> jpaList = new ArrayList<>();
        for (var acInstance : automationCompositionInfoList) {
            for (var element : acInstance.getElements()) {
                var jpa = acElementRepository.getReferenceById(element.getAutomationCompositionElementId().toString());
                jpa.setUseState(element.getUseState());
                jpa.setOperationalState(element.getOperationalState());
                jpa.setOutProperties(element.getOutProperties());
                jpaList.add(jpa);
            }
        }
        acElementRepository.saveAll(jpaList);
    }

    /**
     * Update AutomationCompositionElement.
     *
     * @param element the AutomationCompositionElement
     */
    public void updateAutomationCompositionElement(@NonNull final AutomationCompositionElement element) {
        var jpaAcElement = acElementRepository.getReferenceById(element.getId().toString());
        jpaAcElement.setMessage(element.getMessage());
        jpaAcElement.setOutProperties(element.getOutProperties());
        jpaAcElement.setOperationalState(element.getOperationalState());
        jpaAcElement.setUseState(element.getUseState());
        jpaAcElement.setDeployState(element.getDeployState());
        jpaAcElement.setLockState(element.getLockState());
        jpaAcElement.setSubState(element.getSubState());
        jpaAcElement.setRestarting(element.getRestarting());

        ProviderUtils.validate(element, jpaAcElement, "AutomationCompositionElement");
        acElementRepository.save(jpaAcElement);
    }

    /**
     * Delete AutomationCompositionElement.
     *
     * @param elementId the AutomationCompositionElement Id
     */
    public void deleteAutomationCompositionElement(@NonNull final UUID elementId) {
        acElementRepository.deleteById(elementId.toString());
    }

    /**
     * Validate ElementIds.
     *
     * @param automationComposition the AutomationComposition
     * @return the BeanValidationResult
     */
    public BeanValidationResult validateElementIds(final AutomationComposition automationComposition) {
        var result = new BeanValidationResult(
            "UUID elements " + automationComposition.getName(), automationComposition);

        var ids = automationComposition
            .getElements().values().stream().map(AutomationCompositionElement::getId).toList();
        var elements = acElementRepository.findAllById(ids.stream().map(UUID::toString).toList());
        if (automationComposition.getInstanceId() == null) {
            for (var element : elements) {
                result.addResult(
                    element.getDescription(), element.getElementId(), ValidationStatus.INVALID, "UUID already used");
            }
        } else {
            var instanceId = automationComposition.getInstanceId().toString();
            for (var element : elements) {
                if (!instanceId.equals(element.getInstanceId())) {
                    result.addResult(
                        element.getDescription(), element.getElementId(), ValidationStatus.INVALID,
                        "UUID already used");
                }
            }
        }
        return result;
    }
}

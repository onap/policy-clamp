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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import io.opentelemetry.context.Context;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ThreadHandler implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadHandler.class);

    private final AutomationCompositionElementListener listener;
    private final ParticipantIntermediaryApi intermediaryApi;
    private final CacheProvider cacheProvider;

    private final Map<UUID, Future<?>> executionMap = new ConcurrentHashMap<>();

    private final ExecutorService executor;

    /**
     * Constructor.
     *
     * @param listener the AutomationComposition ElementListener
     * @param intermediaryApi the intermediaryApi
     * @param cacheProvider the CacheProvider
     * @param parameters the parameters
     */
    public ThreadHandler(AutomationCompositionElementListener listener, ParticipantIntermediaryApi intermediaryApi,
            CacheProvider cacheProvider, ParticipantParameters parameters) {
        this.listener = listener;
        this.intermediaryApi = intermediaryApi;
        this.cacheProvider = cacheProvider;
        executor = Context.taskWrapping(Executors.newFixedThreadPool(
                parameters.getIntermediaryParameters().getNumThreads()));
    }

    /**
     * Handle a deploy on a automation composition element.
     *
     * @param messageId the messageId
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     */
    public void deploy(UUID messageId, CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        cleanExecution(instanceElement.elementId(), messageId);
        var result = executor.submit(() -> this.deployProcess(compositionElement, instanceElement));
        executionMap.put(instanceElement.elementId(), result);
    }

    private void deployProcess(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        try {
            listener.deploy(compositionElement, instanceElement);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element deploy failed {} {}", instanceElement.elementId(),
                e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                "Automation composition element deploy failed");
        }
        executionMap.remove(instanceElement.elementId());
    }

    /**
     * Handle an udeploy on a automation composition element.
     *
     * @param messageId the messageId
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     */
    public void undeploy(UUID messageId, CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        cleanExecution(instanceElement.elementId(), messageId);
        var result = executor.submit(() -> this.undeployProcess(compositionElement, instanceElement));
        executionMap.put(instanceElement.elementId(), result);
    }

    private void undeployProcess(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        try {
            listener.undeploy(compositionElement, instanceElement);
        } catch (PfModelException e) {
            LOGGER.error(
                "Automation composition element undeploy failed {} {}", instanceElement.elementId(), e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.DEPLOYED, null,
                StateChangeResult.FAILED, "Automation composition element undeploy failed");
        }
        executionMap.remove(instanceElement.elementId());
    }

    /**
     * Handle a automation composition element lock.
     *
     * @param messageId the messageId
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     */
    public void lock(UUID messageId, CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        cleanExecution(instanceElement.elementId(), messageId);
        var result = executor.submit(() -> this.lockProcess(compositionElement, instanceElement));
        executionMap.put(instanceElement.elementId(), result);
    }

    private void lockProcess(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        try {
            listener.lock(compositionElement, instanceElement);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element lock failed {} {}",
                instanceElement.elementId(), e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), null, LockState.UNLOCKED, StateChangeResult.FAILED,
                "Automation composition element lock failed");
        }
        executionMap.remove(instanceElement.elementId());
    }

    /**
     * Handle a automation composition element unlock.
     *
     * @param messageId the messageId
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     */
    public void unlock(UUID messageId, CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        cleanExecution(instanceElement.elementId(), messageId);
        var result = executor.submit(() -> this.unlockProcess(compositionElement, instanceElement));
        executionMap.put(instanceElement.elementId(), result);
    }

    private void unlockProcess(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        try {
            listener.unlock(compositionElement, instanceElement);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element unlock failed {} {}",
                instanceElement.elementId(), e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), null, LockState.LOCKED, StateChangeResult.FAILED,
                "Automation composition element unlock failed");
        }
        executionMap.remove(instanceElement.elementId());
    }

    /**
     * Handle a automation composition element delete.
     *
     * @param messageId the messageId
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     */
    public void delete(UUID messageId, CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        cleanExecution(instanceElement.elementId(), messageId);
        var result = executor.submit(() -> this.deleteProcess(compositionElement, instanceElement));
        executionMap.put(instanceElement.elementId(), result);
    }

    private void deleteProcess(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        try {
            listener.delete(compositionElement, instanceElement);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element delete failed {} {}",
                instanceElement.elementId(), e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(
                instanceElement.instanceId(), instanceElement.elementId(), DeployState.UNDEPLOYED, null,
                StateChangeResult.FAILED, "Automation composition element delete failed");
        }
        executionMap.remove(instanceElement.elementId());
    }

    /**
     * Handle a automation composition element properties update.
     *
     * @param messageId the messageId
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @param instanceElementUpdated the information of the Automation Composition Instance Element updated
     */
    public void update(UUID messageId, CompositionElementDto compositionElement, InstanceElementDto instanceElement,
                       InstanceElementDto instanceElementUpdated) {
        cleanExecution(instanceElement.elementId(), messageId);
        var result = executor.submit(() ->
            this.updateProcess(compositionElement, instanceElement, instanceElementUpdated));
        executionMap.put(instanceElement.elementId(), result);
    }

    private void updateProcess(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
                               InstanceElementDto instanceElementUpdated) {
        try {
            listener.update(compositionElement, instanceElement, instanceElementUpdated);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element update failed {} {}",
                instanceElement.elementId(), e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.DEPLOYED, null,
                StateChangeResult.FAILED, "Automation composition element update failed");
        }
        executionMap.remove(instanceElement.elementId());
    }

    /**
     * Clean Execution.
     *
     * @param execIdentificationId the identification Id
     * @param messageId the messageId
     */
    public void cleanExecution(UUID execIdentificationId, UUID messageId) {
        var process = executionMap.get(execIdentificationId);
        if (process != null) {
            if (!process.isDone()) {
                process.cancel(true);
            }
            executionMap.remove(execIdentificationId);
        }
        cacheProvider.getMsgIdentification().put(execIdentificationId, messageId);
    }

    /**
     * Handles prime a Composition Definition.
     *
     * @param messageId the messageId
     * @param composition the composition
     */
    public void prime(UUID messageId, CompositionDto composition) {
        cleanExecution(composition.compositionId(), messageId);
        var result = executor.submit(() -> this.primeProcess(composition));
        executionMap.put(composition.compositionId(), result);
    }

    private void primeProcess(CompositionDto composition) {
        try {
            listener.prime(composition);
            executionMap.remove(composition.compositionId());
        } catch (PfModelException e) {
            LOGGER.error("Composition Defintion prime failed {} {}", composition.compositionId(), e.getMessage());
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.COMMISSIONED,
                StateChangeResult.FAILED, "Composition Defintion prime failed");
        }
    }

    /**
     * Handles deprime a Composition Definition.
     *
     * @param messageId the messageId
     * @param composition the composition
     */
    public void deprime(UUID messageId, CompositionDto composition) {
        cleanExecution(composition.compositionId(), messageId);
        var result = executor.submit(() -> this.deprimeProcess(composition));
        executionMap.put(composition.compositionId(), result);
    }

    private void deprimeProcess(CompositionDto composition) {
        try {
            listener.deprime(composition);
            executionMap.remove(composition.compositionId());
        } catch (PfModelException e) {
            LOGGER.error("Composition Defintion deprime failed {} {}", composition.compositionId(), e.getMessage());
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.PRIMED,
                StateChangeResult.FAILED, "Composition Defintion deprime failed");
        }
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        executor.shutdown();
    }

    /**
     * Handles AutomationComposition Migration.
     *
     * @param messageId the messageId
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param compositionElementTarget the information of the Automation Composition Definition Element Target
     * @param instanceElement the information of the Automation Composition Instance Element
     * @param instanceElementMigrate the information of the Automation Composition Instance Element updated
     * @param stage the stage
     */
    public void migrate(UUID messageId, CompositionElementDto compositionElement,
        CompositionElementDto compositionElementTarget, InstanceElementDto instanceElement,
        InstanceElementDto instanceElementMigrate, int stage) {
        cleanExecution(instanceElement.elementId(), messageId);
        var result = executor.submit(() ->
            this.migrateProcess(compositionElement, compositionElementTarget,
                instanceElement, instanceElementMigrate, stage));
        executionMap.put(instanceElement.elementId(), result);
    }

    private void migrateProcess(CompositionElementDto compositionElement,
        CompositionElementDto compositionElementTarget, InstanceElementDto instanceElement,
        InstanceElementDto instanceElementMigrate, int stage) {
        try {
            listener.migrate(compositionElement, compositionElementTarget,
                instanceElement, instanceElementMigrate, stage);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element migrate failed {} {}",
                instanceElement.elementId(), e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(
                instanceElement.instanceId(), instanceElement.elementId(), DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, "Automation composition element migrate failed");
        }
        executionMap.remove(instanceElement.elementId());
    }

    /**
     * Handles AutomationComposition Migration Precheck.
     *
     * @param messageId the messageId
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param compositionElementTarget the information of the Automation Composition Definition Element Target
     * @param instanceElement the information of the Automation Composition Instance Element
     * @param instanceElementMigrate the information of the Automation Composition Instance Element updated
     */
    public void migratePrecheck(UUID messageId, CompositionElementDto compositionElement,
        CompositionElementDto compositionElementTarget, InstanceElementDto instanceElement,
        InstanceElementDto instanceElementMigrate) {
        cleanExecution(instanceElement.elementId(), messageId);
        var result = executor.submit(() ->
            this.migratePrecheckProcess(compositionElement, compositionElementTarget, instanceElement,
                instanceElementMigrate));
        executionMap.put(instanceElement.elementId(), result);
    }

    private void migratePrecheckProcess(CompositionElementDto compositionElement,
        CompositionElementDto compositionElementTarget, InstanceElementDto instanceElement,
        InstanceElementDto instanceElementMigrate) {
        try {
            listener.migratePrecheck(compositionElement, compositionElementTarget, instanceElement,
                instanceElementMigrate);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element migrate precheck failed {} {}",
                instanceElement.elementId(), e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(
                instanceElement.instanceId(), instanceElement.elementId(), DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, "Automation composition element migrate precheck failed");
        }
        executionMap.remove(instanceElement.elementId());
    }

    /**
     * Handles AutomationComposition Prepare Post Deploy.
     *
     * @param messageId the messageId
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     */
    public void review(UUID messageId, CompositionElementDto compositionElement,
        InstanceElementDto instanceElement) {
        cleanExecution(instanceElement.elementId(), messageId);
        var result = executor.submit(() -> this.reviewProcess(compositionElement, instanceElement));
        executionMap.put(instanceElement.elementId(), result);
    }

    private void reviewProcess(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        try {
            listener.review(compositionElement, instanceElement);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element Review failed {} {}",
                instanceElement.elementId(), e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(
                instanceElement.instanceId(), instanceElement.elementId(), DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, "Automation composition element Review failed");
        }
        executionMap.remove(instanceElement.elementId());
    }

    /**
     * Handles AutomationComposition Prepare Pre Deploy.
     *
     * @param messageId the messageId
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @param stage the stage
     */
    public void prepare(UUID messageId, CompositionElementDto compositionElement,
        InstanceElementDto instanceElement, int stage) {
        cleanExecution(instanceElement.elementId(), messageId);
        var result = executor.submit(() -> this.prepareProcess(compositionElement, instanceElement, stage));
        executionMap.put(instanceElement.elementId(), result);
    }

    private void prepareProcess(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
        int stage) {
        try {
            listener.prepare(compositionElement, instanceElement, stage);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element prepare Pre Deploy failed {} {}",
                instanceElement.elementId(), e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(
                instanceElement.instanceId(), instanceElement.elementId(), DeployState.UNDEPLOYED,
                null, StateChangeResult.FAILED, "Automation composition element prepare Pre Deploy failed");
        }
        executionMap.remove(instanceElement.elementId());
    }
}

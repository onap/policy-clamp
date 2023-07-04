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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcElementRestart;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThreadHandler implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadHandler.class);

    private final AutomationCompositionElementListener listener;
    private final ParticipantIntermediaryApi intermediaryApi;
    private final CacheProvider cacheProvider;

    private final Map<UUID, Future> executionMap = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * Handle an update on a automation composition element.
     *
     * @param messageId the messageId
     * @param instanceId the automationComposition Id
     * @param element the information on the automation composition element
     * @param properties properties Map
     */
    public void deploy(UUID messageId, UUID instanceId, AcElementDeploy element, Map<String, Object> properties) {
        cleanExecution(element.getId(), messageId);
        var result = executor.submit(() -> this.deployProcess(instanceId, element, properties));
        executionMap.put(element.getId(), result);
    }

    private void deployProcess(UUID instanceId, AcElementDeploy element, Map<String, Object> properties) {
        try {
            listener.deploy(instanceId, element, properties);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element deploy failed {} {}", instanceId, e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(instanceId, element.getId(), DeployState.UNDEPLOYED,
                    null, StateChangeResult.FAILED, "Automation composition element deploy failed");
        }
        executionMap.remove(element.getId());
    }

    /**
     * Handle a automation composition element state change.
     *
     * @param messageId the messageId
     * @param instanceId the automationComposition Id
     * @param elementId the ID of the automation composition element
     */
    public void undeploy(UUID messageId, UUID instanceId, UUID elementId) {
        cleanExecution(elementId, messageId);
        var result = executor.submit(() -> this.undeployProcess(instanceId, elementId));
        executionMap.put(elementId, result);
    }

    private void undeployProcess(UUID instanceId, UUID elementId) {
        try {
            listener.undeploy(instanceId, elementId);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element undeploy failed {} {}", instanceId, e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, DeployState.DEPLOYED, null,
                    StateChangeResult.FAILED, "Automation composition element undeploy failed");
        }
        executionMap.remove(elementId);
    }

    /**
     * Handle a automation composition element lock.
     *
     * @param messageId the messageId
     * @param instanceId the automationComposition Id
     * @param elementId the ID of the automation composition element
     */
    public void lock(UUID messageId, UUID instanceId, UUID elementId) {
        cleanExecution(elementId, messageId);
        var result = executor.submit(() -> this.lockProcess(instanceId, elementId));
        executionMap.put(elementId, result);
    }

    private void lockProcess(UUID instanceId, UUID elementId) {
        try {
            listener.lock(instanceId, elementId);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element lock failed {} {}", instanceId, e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.UNLOCKED,
                    StateChangeResult.FAILED, "Automation composition element lock failed");
        }
        executionMap.remove(elementId);
    }

    /**
     * Handle a automation composition element unlock.
     *
     * @param messageId the messageId
     * @param instanceId the automationComposition Id
     * @param elementId the ID of the automation composition element
     */
    public void unlock(UUID messageId, UUID instanceId, UUID elementId) {
        cleanExecution(elementId, messageId);
        var result = executor.submit(() -> this.unlockProcess(instanceId, elementId));
        executionMap.put(elementId, result);
    }

    private void unlockProcess(UUID instanceId, UUID elementId) {
        try {
            listener.unlock(instanceId, elementId);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element unlock failed {} {}", instanceId, e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.LOCKED,
                    StateChangeResult.FAILED, "Automation composition element unlock failed");
        }
        executionMap.remove(elementId);
    }

    /**
     * Handle a automation composition element delete.
     *
     * @param messageId the messageId
     * @param instanceId the automationComposition Id
     * @param elementId the ID of the automation composition element
     */
    public void delete(UUID messageId, UUID instanceId, UUID elementId) {
        cleanExecution(elementId, messageId);
        var result = executor.submit(() -> this.deleteProcess(instanceId, elementId));
        executionMap.put(elementId, result);
    }

    private void deleteProcess(UUID instanceId, UUID elementId) {
        try {
            listener.delete(instanceId, elementId);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element delete failed {} {}", instanceId, e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, DeployState.UNDEPLOYED, null,
                    StateChangeResult.FAILED, "Automation composition element delete failed");
        }
        executionMap.remove(elementId);
    }

    /**
     * Handle a automation composition element properties update.
     *
     * @param messageId the messageId
     * @param instanceId the automationComposition Id
     * @param element the information on the automation composition element
     * @param properties properties Map
     */
    public void update(UUID messageId, UUID instanceId, AcElementDeploy element, Map<String, Object> properties) {
        cleanExecution(element.getId(), messageId);
        var result = executor.submit(() -> this.updateProcess(instanceId, element, properties));
        executionMap.put(element.getId(), result);
    }

    private void updateProcess(UUID instanceId, AcElementDeploy element, Map<String, Object> properties) {
        try {
            listener.update(instanceId, element, properties);
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element update failed {} {}", instanceId, e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(instanceId, element.getId(), DeployState.DEPLOYED,
                    null, StateChangeResult.FAILED, "Automation composition element update failed");
        }
        executionMap.remove(element.getId());
    }

    private void cleanExecution(UUID execIdentificationId, UUID messageId) {
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
     * @param compositionId the compositionId
     * @param list the list of AutomationCompositionElementDefinition
     */
    public void prime(UUID messageId, UUID compositionId, List<AutomationCompositionElementDefinition> list) {
        cleanExecution(compositionId, messageId);
        var result = executor.submit(() -> this.primeProcess(compositionId, list));
        executionMap.put(compositionId, result);
    }

    private void primeProcess(UUID compositionId, List<AutomationCompositionElementDefinition> list) {
        try {
            listener.prime(compositionId, list);
            executionMap.remove(compositionId);
        } catch (PfModelException e) {
            LOGGER.error("Composition Defintion prime failed {} {}", compositionId, e.getMessage());
            intermediaryApi.updateCompositionState(compositionId, AcTypeState.COMMISSIONED, StateChangeResult.FAILED,
                    "Composition Defintion prime failed");
        }
    }

    /**
     * Handles deprime a Composition Definition.
     *
     * @param messageId the messageId
     * @param compositionId the compositionId
     */
    public void deprime(UUID messageId, UUID compositionId) {
        cleanExecution(compositionId, messageId);
        var result = executor.submit(() -> this.deprimeProcess(compositionId));
        executionMap.put(compositionId, result);
    }

    private void deprimeProcess(UUID compositionId) {
        try {
            listener.deprime(compositionId);
            executionMap.remove(compositionId);
        } catch (PfModelException e) {
            LOGGER.error("Composition Defintion deprime failed {} {}", compositionId, e.getMessage());
            intermediaryApi.updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.FAILED,
                    "Composition Defintion deprime failed");
        }
    }

<<<<<<< HEAD   (358a9a Fix OFF_LINE issue when Status message upcoming)
=======
    /**
     * Handles restarted scenario.
     *
     * @param messageId the messageId
     * @param compositionId the compositionId
     * @param list the list of AutomationCompositionElementDefinition
     * @param state the state of the composition
     * @param automationCompositionList list of ParticipantRestartAc
     */
    public void restarted(UUID messageId, UUID compositionId, List<AutomationCompositionElementDefinition> list,
            AcTypeState state, List<ParticipantRestartAc> automationCompositionList) {
        try {
            listener.handleRestartComposition(compositionId, list, state);
        } catch (PfModelException e) {
            LOGGER.error("Composition Defintion restarted failed {} {}", compositionId, e.getMessage());
            intermediaryApi.updateCompositionState(compositionId, state, StateChangeResult.FAILED,
                    "Composition Defintion restarted failed");
        }

        for (var automationComposition : automationCompositionList) {
            for (var element : automationComposition.getAcElementList()) {
                cleanExecution(element.getId(), messageId);
                var result = executor.submit(() -> this
                        .restartedInstanceProcess(automationComposition.getAutomationCompositionId(), element));
                executionMap.put(element.getId(), result);
            }
        }
    }

    private void restartedInstanceProcess(UUID instanceId, AcElementRestart element) {
        try {
            var map = new HashMap<>(cacheProvider.getCommonProperties(instanceId, element.getId()));
            map.putAll(element.getProperties());

            listener.handleRestartInstance(instanceId, getAcElementDeploy(element), map, element.getDeployState(),
                    element.getLockState());
            executionMap.remove(element.getId());
        } catch (PfModelException e) {
            LOGGER.error("Automation composition element deploy failed {} {}", instanceId, e.getMessage());
            intermediaryApi.updateAutomationCompositionElementState(instanceId, element.getId(),
                    element.getDeployState(), element.getLockState(), StateChangeResult.FAILED,
                    "Automation composition element restart failed");
        }
    }

    private AcElementDeploy getAcElementDeploy(AcElementRestart element) {
        var acElementDeploy = new AcElementDeploy();
        acElementDeploy.setId(element.getId());
        acElementDeploy.setDefinition(element.getDefinition());
        acElementDeploy.setProperties(element.getProperties());
        acElementDeploy.setToscaServiceTemplateFragment(element.getToscaServiceTemplateFragment());
        return acElementDeploy;
    }
>>>>>>> CHANGE (ef0a60 Add restart support inside participants)

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
}

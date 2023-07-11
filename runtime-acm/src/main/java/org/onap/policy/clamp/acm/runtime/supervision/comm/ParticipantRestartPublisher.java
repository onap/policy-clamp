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

package org.onap.policy.clamp.acm.runtime.supervision.comm;

import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantRestartAc;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRestart;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ParticipantRestartPublisher extends AbstractParticipantPublisher<ParticipantRestart> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantRestartPublisher.class);

    /**
     * Send Restart to Participant.
     *
     * @param participantId the ParticipantId
     * @param acmDefinition the AutomationComposition Definition
     * @param automationCompositions the list of automationCompositions
     */
    @Timed(value = "publisher.participant_restart", description = "Participant Restart published")
    public void send(UUID participantId, AutomationCompositionDefinition acmDefinition,
            List<AutomationComposition> automationCompositions) {

        var message = new ParticipantRestart();
        message.setParticipantId(participantId);
        message.setCompositionId(acmDefinition.getCompositionId());
        message.setMessageId(UUID.randomUUID());
        message.setTimestamp(Instant.now());
        message.setState(acmDefinition.getState());
        message.setParticipantDefinitionUpdates(prepareParticipantRestarting(participantId, acmDefinition));
        var toscaServiceTemplateFragment = AcmUtils.getToscaServiceTemplateFragment(acmDefinition.getServiceTemplate());

        for (var automationComposition : automationCompositions) {
            var restartAc = new ParticipantRestartAc();
            restartAc.setAutomationCompositionId(automationComposition.getInstanceId());
            for (var element : automationComposition.getElements().values()) {
                if (participantId.equals(element.getParticipantId())) {
                    var acElementRestart = AcmUtils.createAcElementRestart(element);
                    acElementRestart.setToscaServiceTemplateFragment(toscaServiceTemplateFragment);
                    restartAc.getAcElementList().add(acElementRestart);
                }
            }
            message.getAutomationcompositionList().add(restartAc);
        }

        LOGGER.debug("Participant Restart sent {}", message);
        super.send(message);
    }

    private List<ParticipantDefinition> prepareParticipantRestarting(UUID participantId,
            AutomationCompositionDefinition acmDefinition) {
        var acElements = AcmUtils.extractAcElementsFromServiceTemplate(acmDefinition.getServiceTemplate());

        // list of entry entry filtered by participantId
        List<Entry<String, ToscaNodeTemplate>> elementList = new ArrayList<>();
        Map<ToscaConceptIdentifier, UUID> supportedElementMap = new HashMap<>();
        for (var elementEntry : acElements) {
            var elementState = acmDefinition.getElementStateMap().get(elementEntry.getKey());
            if (participantId.equals(elementState.getParticipantId())) {
                var type = new ToscaConceptIdentifier(elementEntry.getValue().getType(),
                        elementEntry.getValue().getTypeVersion());
                supportedElementMap.put(type, participantId);
                elementList.add(elementEntry);
            }
        }
        return AcmUtils.prepareParticipantPriming(elementList, supportedElementMap);
    }
}

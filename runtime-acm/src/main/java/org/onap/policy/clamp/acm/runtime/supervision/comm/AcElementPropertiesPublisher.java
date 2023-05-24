/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
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
import java.util.UUID;
import java.util.function.UnaryOperator;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to send PropertiesUpdate messages to participants.
 */
@Component
@AllArgsConstructor
public class AcElementPropertiesPublisher extends AbstractParticipantPublisher<PropertiesUpdate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionDeployPublisher.class);

    /**
     * Send ACElementPropertiesUpdate to Participant.
     *
     * @param automationComposition the AutomationComposition
     */
    @Timed(value = "publisher.properties_update",
            description = "AC Element Properties Update published")
    public void send(AutomationComposition automationComposition) {
        Map<UUID, List<AcElementDeploy>> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            var acElementDeploy = new AcElementDeploy();
            acElementDeploy.setId(element.getId());
            acElementDeploy.setDefinition(new ToscaConceptIdentifier(element.getDefinition()));
            acElementDeploy.setOrderedState(DeployOrder.UPDATE);
            acElementDeploy.setProperties(PfUtils.mapMap(element.getProperties(), UnaryOperator.identity()));

            map.putIfAbsent(element.getParticipantId(), new ArrayList<>());
            map.get(element.getParticipantId()).add(acElementDeploy);
        }
        List<ParticipantDeploy> participantDeploys = new ArrayList<>();
        for (var entry : map.entrySet()) {
            var participantDeploy = new ParticipantDeploy();
            participantDeploy.setParticipantId(entry.getKey());
            participantDeploy.setAcElementList(entry.getValue());
            participantDeploys.add(participantDeploy);
        }

        var propertiesUpdate = new PropertiesUpdate();
        propertiesUpdate.setCompositionId(automationComposition.getCompositionId());
        propertiesUpdate.setAutomationCompositionId(automationComposition.getInstanceId());
        propertiesUpdate.setMessageId(UUID.randomUUID());
        propertiesUpdate.setTimestamp(Instant.now());
        propertiesUpdate.setParticipantUpdatesList(participantDeploys);

        LOGGER.debug("AC Element properties update sent {}", propertiesUpdate);
        super.send(propertiesUpdate);
    }
}

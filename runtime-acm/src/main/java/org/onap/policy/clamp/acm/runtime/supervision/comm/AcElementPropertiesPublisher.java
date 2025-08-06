/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to send PropertiesUpdate messages to participants.
 */
@Component
@AllArgsConstructor
public class AcElementPropertiesPublisher extends AbstractParticipantPublisher<PropertiesUpdate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcElementPropertiesPublisher.class);

    /**
     * Send ACElementPropertiesUpdate to Participant.
     *
     * @param automationComposition the AutomationComposition
     * @param revisionIdComposition the last Update from Composition
     */
    @Timed(value = "publisher.properties_update", description = "AC Element Properties Update published")
    public void send(AutomationComposition automationComposition, UUID revisionIdComposition) {
        var propertiesUpdate = new PropertiesUpdate();
        propertiesUpdate.setCompositionId(automationComposition.getCompositionId());
        propertiesUpdate.setAutomationCompositionId(automationComposition.getInstanceId());
        propertiesUpdate.setMessageId(UUID.randomUUID());
        propertiesUpdate.setTimestamp(Instant.now());
        propertiesUpdate.setRevisionIdInstance(automationComposition.getRevisionId());
        propertiesUpdate.setRevisionIdComposition(revisionIdComposition);
        var participantUpdatesList = AcmUtils.createParticipantDeployList(automationComposition, DeployOrder.UPDATE,
                List.of());
        propertiesUpdate.setParticipantUpdatesList(participantUpdatesList);
        propertiesUpdate.setParticipantIdList(participantUpdatesList.stream()
                .map(ParticipantDeploy::getParticipantId).collect(Collectors.toSet()));
        LOGGER.debug("AC Element properties update sent {}", propertiesUpdate.getMessageId());
        super.send(propertiesUpdate);
    }
}

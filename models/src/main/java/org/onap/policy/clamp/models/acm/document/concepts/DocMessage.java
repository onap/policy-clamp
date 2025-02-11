/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.document.concepts;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class DocMessage {
    private String messageId;

    private ParticipantMessageType messageType;
    private UUID participantId;
    private UUID replicaId;

    private UUID compositionId;
    private AcTypeState compositionState;
    private StateChangeResult stateChangeResult;

    private UUID instanceId;
    private UUID instanceElementId;
    private DeployState deployState;
    private LockState lockState;
    private String message;
    private Integer stage;

    private ToscaConceptIdentifier acElementDefinitionId;
    private Map<String, Object> outProperties = new LinkedHashMap<>();
    private String operationalState;
    private String useState;

    /**
     * Constructor.
     *
     * @param copy copy Constructor
     */
    public DocMessage(DocMessage copy) {
        this.messageId = copy.messageId;
        this.messageType = copy.messageType;
        this.participantId = copy.participantId;
        this.replicaId = copy.replicaId;
        this.compositionId = copy.compositionId;
        this.compositionState = copy.compositionState;
        this.stateChangeResult = copy.stateChangeResult;
        this.instanceId = copy.instanceId;
        this.instanceElementId = copy.instanceElementId;
        this.deployState = copy.deployState;
        this.lockState = copy.lockState;
        this.message = copy.message;
        this.stage = copy.stage;
        this.acElementDefinitionId = copy.acElementDefinitionId;
        this.outProperties = AcmUtils.cloneMap(copy.outProperties);
        this.operationalState = copy.operationalState;
        this.useState = copy.useState;
    }
}

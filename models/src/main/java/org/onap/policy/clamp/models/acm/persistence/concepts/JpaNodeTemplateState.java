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

package org.onap.policy.clamp.models.acm.persistence.concepts;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.Validated;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

@Entity
@Table(name = "NodeTemplateState")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@EqualsAndHashCode(callSuper = false)
public class JpaNodeTemplateState extends Validated implements PfAuthorative<NodeTemplateState> {

    @Id
    @NotNull
    private String nodeTemplateStateId;

    @Column
    @NotNull
    private String compositionId;

    @Column
    private String participantId;

    @Column
    @NotNull
    private ToscaConceptIdentifier nodeTemplateId;

    @Column
    @NotNull
    private AcTypeState state;

    /**
     * The Default Constructor.
     */
    public JpaNodeTemplateState() {
        this(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    /**
     * Constructor.
     *
     * @param nodeTemplateStateId the nodeTemplateStateId
     * @param compositionId the compositionId
     */
    public JpaNodeTemplateState(@NotNull String nodeTemplateStateId, @NotNull String compositionId) {
        this.nodeTemplateStateId = nodeTemplateStateId;
        this.compositionId = compositionId;
    }

    @Override
    public void fromAuthorative(NodeTemplateState copyConcept) {
        this.nodeTemplateStateId = copyConcept.getNodeTemplateStateId().toString();
        if (copyConcept.getParticipantId() != null) {
            this.participantId = copyConcept.getParticipantId().toString();
        }
        this.nodeTemplateId = copyConcept.getNodeTemplateId();
    }

    @Override
    public NodeTemplateState toAuthorative() {
        var nodeTemplateState = new NodeTemplateState();
        nodeTemplateState.setNodeTemplateStateId(UUID.fromString(this.nodeTemplateStateId));
        if (this.participantId != null) {
            nodeTemplateState.setParticipantId(UUID.fromString(this.participantId));
        }
        nodeTemplateState.setNodeTemplateId(this.nodeTemplateId);
        nodeTemplateState.setState(this.state);
        return nodeTemplateState;
    }
}

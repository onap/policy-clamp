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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.base.Validated;
import org.onap.policy.models.base.validation.annotations.VerifyKey;
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

    @VerifyKey
    @NotNull
    @AttributeOverride(name = "name",    column = @Column(name = "nodeTemplate_name"))
    @AttributeOverride(name = "version", column = @Column(name = "nodeTemplate_version"))
    private PfConceptKey nodeTemplateId;

    @Column
    private Boolean restarting;

    @Column
    @NotNull
    private AcTypeState state;

    @Column
    private String message;

    @Lob
    @NotNull
    @Valid
    @Convert(converter = StringToMapConverter.class)
    @Column(length = 100000)
    private Map<String, Object> outProperties;

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
        this.nodeTemplateId = copyConcept.getNodeTemplateId().asConceptKey();
        this.restarting = copyConcept.getRestarting();
        this.state = copyConcept.getState();
        this.message = copyConcept.getMessage();
        this.outProperties = PfUtils.mapMap(copyConcept.getOutProperties(), UnaryOperator.identity());
    }

    @Override
    public NodeTemplateState toAuthorative() {
        var nodeTemplateState = new NodeTemplateState();
        nodeTemplateState.setNodeTemplateStateId(UUID.fromString(this.nodeTemplateStateId));
        if (this.participantId != null) {
            nodeTemplateState.setParticipantId(UUID.fromString(this.participantId));
        }
        nodeTemplateState.setNodeTemplateId(new ToscaConceptIdentifier(this.nodeTemplateId));
        nodeTemplateState.setRestarting(this.restarting);
        nodeTemplateState.setState(this.state);
        nodeTemplateState.setMessage(this.message);
        nodeTemplateState.setOutProperties(PfUtils.mapMap(outProperties, UnaryOperator.identity()));
        return nodeTemplateState;
    }
}

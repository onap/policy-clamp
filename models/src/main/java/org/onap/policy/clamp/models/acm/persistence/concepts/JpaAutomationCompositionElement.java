/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.persistence.concepts;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.base.Validated;
import org.onap.policy.models.base.validation.annotations.VerifyKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent a participant automation composition element in the database.
 *
 * @author Liam Fallon (liam.fallon@est.tech)
 */
@Entity
@Table(name = "AutomationCompositionElement")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@EqualsAndHashCode(callSuper = false)
public class JpaAutomationCompositionElement extends Validated
    implements PfAuthorative<AutomationCompositionElement>, Comparable<JpaAutomationCompositionElement> {

    @Id
    @NotNull
    private String elementId;

    @Column
    @NotNull
    private String instanceId;

    @VerifyKey
    @NotNull
    @AttributeOverride(name = "name",    column = @Column(name = "definition_name"))
    @AttributeOverride(name = "version", column = @Column(name = "definition_version"))
    private PfConceptKey definition;

    @Column
    @NotNull
    private String participantId;

    @Column
    @NotNull
    private DeployState deployState;

    @Column
    @NotNull
    private LockState lockState;

    @Column
    @NotNull
    private SubState subState;

    @Column
    private String operationalState;

    @Column
    private String useState;

    @Column
    private Integer stage;

    @Column
    private String description;

    @Column
    private String message;

    @NotNull
    @Valid
    @Convert(converter = StringToMapConverter.class)
    @Column(length = 100000)
    private Map<String, Object> properties;

    @NotNull
    @Valid
    @Convert(converter = StringToMapConverter.class)
    @Column(length = 100000)
    private Map<String, Object> outProperties;

    /**
     * The Default Constructor creates a {@link JpaAutomationCompositionElement} object with a null key.
     */
    public JpaAutomationCompositionElement() {
        this(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    /**
     * The Key Constructor creates a {@link JpaAutomationCompositionElement} object with the given concept key.
     *
     * @param elementId The id of the automation composition instance Element
     * @param instanceId The id of the automation composition instance
     */
    public JpaAutomationCompositionElement(@NonNull final String elementId, @NonNull final String instanceId) {
        this(elementId, instanceId, new PfConceptKey(),
            DeployState.UNDEPLOYED, LockState.NONE, SubState.NONE);
    }

    /**
     * The Key Constructor creates a {@link JpaAutomationCompositionElement} object with all mandatory fields.
     *
     * @param elementId The id of the automation composition instance Element
     * @param instanceId The id of the automation composition instance
     * @param definition the TOSCA definition of the automation composition element
     * @param deployState the Deploy State of the automation composition
     * @param lockState the Lock State of the automation composition
     * @param subState the Sub State of the automation composition
     */
    public JpaAutomationCompositionElement(@NonNull final String elementId, @NonNull final String instanceId,
            @NonNull final PfConceptKey definition,
            @NonNull final DeployState deployState, @NonNull final LockState lockState,
            @NonNull final SubState subState) {
        this.elementId = elementId;
        this.instanceId = instanceId;
        this.definition = definition;
        this.deployState = deployState;
        this.lockState = lockState;
        this.subState = subState;
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaAutomationCompositionElement(@NonNull final JpaAutomationCompositionElement copyConcept) {
        this.elementId = copyConcept.elementId;
        this.instanceId = copyConcept.instanceId;
        this.definition = new PfConceptKey(copyConcept.definition);
        this.participantId = copyConcept.participantId;
        this.description = copyConcept.description;
        this.properties = PfUtils.mapMap(copyConcept.properties, UnaryOperator.identity());
        this.outProperties = PfUtils.mapMap(copyConcept.outProperties, UnaryOperator.identity());
        this.deployState = copyConcept.deployState;
        this.lockState = copyConcept.lockState;
        this.subState = copyConcept.subState;
        this.operationalState = copyConcept.operationalState;
        this.useState = copyConcept.useState;
        this.stage = copyConcept.stage;
        this.message = copyConcept.message;
    }

    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public JpaAutomationCompositionElement(@NonNull final AutomationCompositionElement authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
    }

    @Override
    public AutomationCompositionElement toAuthorative() {
        var element = new AutomationCompositionElement();

        element.setId(UUID.fromString(elementId));
        element.setDefinition(new ToscaConceptIdentifier(definition));
        element.setParticipantId(UUID.fromString(participantId));
        element.setDescription(description);
        element.setProperties(PfUtils.mapMap(properties, UnaryOperator.identity()));
        element.setOutProperties(PfUtils.mapMap(outProperties, UnaryOperator.identity()));
        element.setDeployState(deployState);
        element.setLockState(lockState);
        element.setSubState(subState);
        element.setOperationalState(operationalState);
        element.setUseState(useState);
        element.setStage(stage);
        element.setMessage(message);

        return element;
    }

    @Override
    public void fromAuthorative(@NonNull final AutomationCompositionElement element) {
        this.definition = element.getDefinition().asConceptKey();
        this.participantId = element.getParticipantId().toString();
        this.description = element.getDescription();
        this.properties = PfUtils.mapMap(element.getProperties(), UnaryOperator.identity());
        this.outProperties = PfUtils.mapMap(element.getOutProperties(), UnaryOperator.identity());
        this.deployState = element.getDeployState();
        this.lockState = element.getLockState();
        this.subState = element.getSubState();
        this.operationalState = element.getOperationalState();
        this.useState = element.getUseState();
        this.stage = element.getStage();
        this.message = element.getMessage();
    }

    @Override
    public int compareTo(final JpaAutomationCompositionElement other) {
        if (other == null) {
            return -1;
        }
        if (this == other) {
            return 0;
        }

        var result = ObjectUtils.compare(elementId, other.elementId);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(instanceId, other.instanceId);
        if (result != 0) {
            return result;
        }

        result = definition.compareTo(other.definition);
        if (result != 0) {
            return result;
        }

        result = participantId.compareTo(other.participantId);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(deployState, other.deployState);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(lockState, other.lockState);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(subState, other.subState);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(useState, other.useState);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(stage, other.stage);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(operationalState, other.operationalState);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(message, other.message);
        if (result != 0) {
            return result;
        }
        return ObjectUtils.compare(description, other.description);
    }
}

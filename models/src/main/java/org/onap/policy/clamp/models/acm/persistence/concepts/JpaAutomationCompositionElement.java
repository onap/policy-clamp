/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
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

    // @formatter:off
    @VerifyKey
    @NotNull
    @AttributeOverride(name = "name",    column = @Column(name = "definition_name"))
    @AttributeOverride(name = "version", column = @Column(name = "definition_version"))
    private PfConceptKey definition;

    @VerifyKey
    @NotNull
    @AttributeOverride(name = "name",    column = @Column(name = "participant_type_name"))
    @AttributeOverride(name = "version", column = @Column(name = "participant_type_version"))
    private PfConceptKey participantType;

    @Column
    @NotNull
    private String participantId;

    @Column
    @NotNull
    private AutomationCompositionState state;

    @Column
    @NotNull
    private AutomationCompositionOrderedState orderedState;

    @Column
    private String description;

    @Lob
    @NotNull
    @Valid
    @Convert(converter = StringToMapConverter.class)
    private Map<String, Object> properties;

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
        this(elementId, instanceId, new PfConceptKey(), new PfConceptKey(), AutomationCompositionState.UNINITIALISED);
    }

    /**
     * The Key Constructor creates a {@link JpaAutomationCompositionElement} object with all mandatory fields.
     *
     * @param elementId The id of the automation composition instance Element
     * @param instanceId The id of the automation composition instance
     * @param definition the TOSCA definition of the automation composition element
     * @param participantType the TOSCA definition of the participant running the automation composition element
     * @param state the state of the automation composition
     */
    public JpaAutomationCompositionElement(@NonNull final String elementId, @NonNull final String instanceId,
            @NonNull final PfConceptKey definition, @NonNull final PfConceptKey participantType,
            @NonNull final AutomationCompositionState state) {
        this.elementId = elementId;
        this.instanceId = instanceId;
        this.definition = definition;
        this.participantType = participantType;
        this.state = state;
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
        this.participantType = new PfConceptKey(copyConcept.participantType);
        this.participantId = copyConcept.participantId;
        this.state = copyConcept.state;
        this.orderedState = copyConcept.orderedState;
        this.description = copyConcept.description;
        this.properties = (copyConcept.properties != null ? new LinkedHashMap<>(copyConcept.properties) : null);
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
        element.setParticipantType(new ToscaConceptIdentifier(participantType));
        element.setParticipantId(UUID.fromString(participantId));
        element.setState(state);
        element.setOrderedState(orderedState != null ? orderedState : state.asOrderedState());
        element.setDescription(description);
        element.setProperties(PfUtils.mapMap(properties, UnaryOperator.identity()));

        return element;
    }

    @Override
    public void fromAuthorative(@NonNull final AutomationCompositionElement element) {
        this.definition = element.getDefinition().asConceptKey();
        this.participantType = element.getParticipantType().asConceptKey();
        this.participantId = element.getParticipantId().toString();
        this.state = element.getState();
        this.orderedState = element.getOrderedState();
        this.description = element.getDescription();
        properties = PfUtils.mapMap(element.getProperties(), UnaryOperator.identity());
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

        result = participantType.compareTo(other.participantType);
        if (result != 0) {
            return result;
        }

        result = participantId.compareTo(other.participantId);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(state, other.state);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(orderedState, other.orderedState);
        if (result != 0) {
            return result;
        }

        return ObjectUtils.compare(description, other.description);
    }
}

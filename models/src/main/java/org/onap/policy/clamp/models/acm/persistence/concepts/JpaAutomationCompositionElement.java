/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

import java.util.List;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConcept;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfReferenceKey;
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
public class JpaAutomationCompositionElement extends PfConcept implements PfAuthorative<AutomationCompositionElement> {
    private static final long serialVersionUID = -1791732273187890213L;

    @EmbeddedId
    @VerifyKey
    @NotNull
    private PfReferenceKey key;

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

    @NotNull
    @AttributeOverride(name = "name",    column = @Column(name = "participant_name"))
    @AttributeOverride(name = "version", column = @Column(name = "participant_version"))
    private PfConceptKey participantId;
    // @formatter:on

    @Column
    @NotNull
    private AutomationCompositionState state;

    @Column
    @NotNull
    private AutomationCompositionOrderedState orderedState;

    @Column
    private String description;

    /**
     * The Default Constructor creates a {@link JpaAutomationCompositionElement} object with a null key.
     */
    public JpaAutomationCompositionElement() {
        this(new PfReferenceKey());
    }

    /**
     * The Key Constructor creates a {@link JpaAutomationCompositionElement} object with the given concept key.
     *
     * @param key the key
     */
    public JpaAutomationCompositionElement(@NonNull final PfReferenceKey key) {
        this(key, new PfConceptKey(), new PfConceptKey(), AutomationCompositionState.UNINITIALISED);
    }

    /**
     * The Key Constructor creates a {@link JpaAutomationCompositionElement} object with all mandatory fields.
     *
     * @param key the key
     * @param definition the TOSCA definition of the automation composition element
     * @param participantType the TOSCA definition of the participant running the automation composition element
     * @param state the state of the automation composition
     */
    public JpaAutomationCompositionElement(@NonNull final PfReferenceKey key, @NonNull final PfConceptKey definition,
        @NonNull final PfConceptKey participantType, @NonNull final AutomationCompositionState state) {
        this.key = key;
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
        super(copyConcept);
        this.key = new PfReferenceKey(copyConcept.key);
        this.definition = new PfConceptKey(copyConcept.definition);
        this.participantType = new PfConceptKey(copyConcept.participantType);
        this.participantId = new PfConceptKey(copyConcept.participantId);
        this.state = copyConcept.state;
        this.orderedState = copyConcept.orderedState;
        this.description = copyConcept.description;
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

        element.setId(UUID.fromString(getKey().getLocalName()));
        element.setDefinition(new ToscaConceptIdentifier(definition));
        element.setParticipantType(new ToscaConceptIdentifier(participantType));
        element.setParticipantId(new ToscaConceptIdentifier(participantId));
        element.setState(state);
        element.setOrderedState(orderedState != null ? orderedState : state.asOrderedState());
        element.setDescription(description);

        return element;
    }

    @Override
    public void fromAuthorative(@NonNull final AutomationCompositionElement element) {
        if (this.key == null || this.getKey().isNullKey()) {
            this.setKey(new PfReferenceKey());
            getKey().setLocalName(element.getId().toString());
        }

        this.definition = element.getDefinition().asConceptKey();
        this.participantType = element.getParticipantType().asConceptKey();
        this.participantId = element.getParticipantId().asConceptKey();
        this.state = element.getState();
        this.orderedState = element.getOrderedState();
        this.description = element.getDescription();
    }

    @Override
    public List<PfKey> getKeys() {
        List<PfKey> keyList = getKey().getKeys();

        keyList.add(definition);
        keyList.add(participantType);
        keyList.add(participantId);

        return keyList;
    }

    @Override
    public void clean() {
        key.clean();
        definition.clean();
        participantType.clean();
        participantId.clean();

        if (description != null) {
            description = description.trim();
        }
    }

    @Override
    public int compareTo(final PfConcept otherConcept) {
        if (otherConcept == null) {
            return -1;
        }
        if (this == otherConcept) {
            return 0;
        }
        if (getClass() != otherConcept.getClass()) {
            return this.getClass().getName().compareTo(otherConcept.getClass().getName());
        }

        final JpaAutomationCompositionElement other = (JpaAutomationCompositionElement) otherConcept;
        int result = key.compareTo(other.key);
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

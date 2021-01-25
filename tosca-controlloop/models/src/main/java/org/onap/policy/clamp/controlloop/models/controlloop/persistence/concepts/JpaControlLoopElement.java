/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.controlloop.persistence.concepts;

import java.util.List;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
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
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConcept;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfReferenceKey;
import org.onap.policy.models.base.validation.annotations.VerifyKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent a participant control loop element in the database.
 *
 * @author Liam Fallon (liam.fallon@est.tech)
 */
@Entity
@Table(name = "ControlLoopElement")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@EqualsAndHashCode(callSuper = false)
public class JpaControlLoopElement extends PfConcept implements PfAuthorative<ControlLoopElement> {
    private static final long serialVersionUID = -1791732273187890213L;

    @EmbeddedId
    @VerifyKey
    @NotNull
    private PfReferenceKey key;

    // @formatter:off
    @VerifyKey
    @NotNull
    @AttributeOverrides({
            @AttributeOverride(name = "name",    column = @Column(name = "definition_name")),
            @AttributeOverride(name = "version", column = @Column(name = "definition_version"))
        }
    )
    private PfConceptKey definition;

    @VerifyKey
    @NotNull
    @AttributeOverrides({
            @AttributeOverride(name = "name",    column = @Column(name = "participant_name")),
            @AttributeOverride(name = "version", column = @Column(name = "participant_version"))
        }
    )
    private PfConceptKey participantId;
    // @formatter:on

    @Column
    @NotNull
    private ControlLoopState state;

    @Column
    @NotNull
    private ControlLoopOrderedState orderedState;

    @Column
    private String description;

    /**
     * The Default Constructor creates a {@link JpaControlLoopElement} object with a null key.
     */
    public JpaControlLoopElement() {
        this(new PfReferenceKey());
    }

    /**
     * The Key Constructor creates a {@link JpaControlLoopElement} object with the given concept key.
     *
     * @param key the key
     */
    public JpaControlLoopElement(@NonNull final PfReferenceKey key) {
        this(key, new PfConceptKey(), new PfConceptKey(), ControlLoopState.UNINITIALISED);
    }

    /**
     * The Key Constructor creates a {@link JpaControlLoopElement} object with all mandatory fields.
     *
     * @param key the key
     * @param definition the TOSCA definition of the control loop element
     * @param participantId the TOSCA definition of the participant running the control loop element
     * @param state the state of the control loop
     */
    public JpaControlLoopElement(@NonNull final PfReferenceKey key, @NonNull final PfConceptKey definition,
            @NonNull final PfConceptKey participantId, @NonNull final ControlLoopState state) {
        this.key = key;
        this.definition = definition;
        this.participantId = participantId;
        this.state = state;
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaControlLoopElement(@NonNull final JpaControlLoopElement copyConcept) {
        super(copyConcept);
        this.key = new PfReferenceKey(copyConcept.key);
        this.definition = new PfConceptKey(copyConcept.definition);
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
    public JpaControlLoopElement(@NonNull final ControlLoopElement authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
    }

    @Override
    public ControlLoopElement toAuthorative() {
        ControlLoopElement element = new ControlLoopElement();

        element.setId(UUID.fromString(getKey().getLocalName()));
        element.setDefinition(new ToscaConceptIdentifier(definition));
        element.setParticipantId(new ToscaConceptIdentifier(participantId));
        element.setState(state);
        element.setOrderedState(orderedState != null ? orderedState : state.asOrderedState());
        element.setDescription(description);

        return element;
    }

    @Override
    public void fromAuthorative(@NonNull final ControlLoopElement element) {
        if (this.key == null || this.getKey().isNullKey()) {
            this.setKey(new PfReferenceKey());
            getKey().setLocalName(element.getId().toString());
        }

        this.definition = element.getDefinition().asConceptKey();
        this.participantId = element.getParticipantId().asConceptKey();
        this.state = element.getState();
        this.orderedState =
                (element.getOrderedState() != null ? element.getOrderedState() : element.getState().asOrderedState());
        this.description = element.getDescription();
    }

    @Override
    public List<PfKey> getKeys() {
        List<PfKey> keyList = getKey().getKeys();

        keyList.add(definition);
        keyList.add(participantId);

        return keyList;
    }

    @Override
    public void clean() {
        key.clean();
        definition.clean();
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

        final JpaControlLoopElement other = (JpaControlLoopElement) otherConcept;
        int result = key.compareTo(other.key);
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

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConcept;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfReferenceKey;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.base.validation.annotations.VerifyKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent a control loop in the database.
 *
 * @author Liam Fallon (liam.fallon@est.tech)
 */
@Entity
@Table(name = "ControlLoop")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@EqualsAndHashCode(callSuper = false)
public class JpaControlLoop extends PfConcept implements PfAuthorative<ControlLoop> {
    private static final long serialVersionUID = -4725410933242154805L;

    @EmbeddedId
    @VerifyKey
    @NotNull
    private PfConceptKey key;

    // @formatter:off
    @VerifyKey
    @NotNull
    @AttributeOverride(name = "name",    column = @Column(name = "definition_name"))
    @AttributeOverride(name = "version", column = @Column(name = "definition_version"))
    private PfConceptKey definition;
    // @formatter:on

    @Column
    @NotNull
    private ControlLoopState state;

    @Column
    @NotNull
    private ControlLoopOrderedState orderedState;

    @Column
    private String description;

    @Column
    private Boolean primed;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @NotNull
    private Map<@NotNull UUID, @NotNull @Valid JpaControlLoopElement> elements;

    /**
     * The Default Constructor creates a {@link JpaControlLoop} object with a null key.
     */
    public JpaControlLoop() {
        this(new PfConceptKey());
    }

    /**
     * The Key Constructor creates a {@link JpaControlLoop} object with the given concept key.
     *
     * @param key the key
     */
    public JpaControlLoop(@NonNull final PfConceptKey key) {
        this(key, new PfConceptKey(), ControlLoopState.UNINITIALISED, new LinkedHashMap<>());
    }

    /**
     * The Key Constructor creates a {@link JpaControlLoop} object with all mandatory fields.
     *
     * @param key the key
     * @param definition the TOSCA definition of the control loop
     * @param state the state of the control loop
     * @param elements the elements of the control looop in participants
     */
    public JpaControlLoop(@NonNull final PfConceptKey key, @NonNull final PfConceptKey definition,
            @NonNull final ControlLoopState state, @NonNull final Map<UUID, JpaControlLoopElement> elements) {
        this.key = key;
        this.definition = definition;
        this.state = state;
        this.elements = elements;
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaControlLoop(@NonNull final JpaControlLoop copyConcept) {
        super(copyConcept);
        this.key = new PfConceptKey(copyConcept.key);
        this.definition = new PfConceptKey(copyConcept.definition);
        this.state = copyConcept.state;
        this.orderedState = copyConcept.orderedState;
        this.description = copyConcept.description;
        this.elements = PfUtils.mapMap(copyConcept.elements, JpaControlLoopElement::new, new LinkedHashMap<>(0));
        this.primed = copyConcept.primed;
    }

    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public JpaControlLoop(@NonNull final ControlLoop authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
    }

    @Override
    public ControlLoop toAuthorative() {
        var controlLoop = new ControlLoop();

        controlLoop.setName(getKey().getName());
        controlLoop.setVersion(getKey().getVersion());
        controlLoop.setDefinition(new ToscaConceptIdentifier(definition));
        controlLoop.setState(state);
        controlLoop.setOrderedState(orderedState != null ? orderedState : state.asOrderedState());
        controlLoop.setDescription(description);
        controlLoop.setElements(PfUtils.mapMap(elements, JpaControlLoopElement::toAuthorative, new LinkedHashMap<>(0)));
        controlLoop.setPrimed(primed);

        return controlLoop;
    }

    @Override
    public void fromAuthorative(@NonNull final ControlLoop controlLoop) {
        if (this.key == null || this.getKey().isNullKey()) {
            this.setKey(new PfConceptKey(controlLoop.getName(), controlLoop.getVersion()));
        }

        this.definition = controlLoop.getDefinition().asConceptKey();
        this.state = controlLoop.getState();
        this.orderedState = controlLoop.getOrderedState();
        this.description = controlLoop.getDescription();
        this.primed = controlLoop.getPrimed();

        this.elements = new LinkedHashMap<>(controlLoop.getElements().size());
        for (Entry<UUID, ControlLoopElement> elementEntry : controlLoop.getElements().entrySet()) {
            var jpaControlLoopElement = new JpaControlLoopElement();
            jpaControlLoopElement.setKey(new PfReferenceKey(getKey(), elementEntry.getValue().getId().toString()));
            jpaControlLoopElement.fromAuthorative(elementEntry.getValue());
            this.elements.put(elementEntry.getKey(), jpaControlLoopElement);
        }
    }

    @Override
    public List<PfKey> getKeys() {
        List<PfKey> keyList = getKey().getKeys();

        keyList.add(definition);

        for (JpaControlLoopElement element : elements.values()) {
            keyList.addAll(element.getKeys());
        }

        return keyList;
    }

    @Override
    public void clean() {
        key.clean();
        definition.clean();
        description = (description == null ? null : description.trim());

        for (JpaControlLoopElement element : elements.values()) {
            element.clean();
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

        final JpaControlLoop other = (JpaControlLoop) otherConcept;
        int result = key.compareTo(other.key);
        if (result != 0) {
            return result;
        }

        result = definition.compareTo(other.definition);
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

        result = ObjectUtils.compare(description, other.description);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(primed, other.primed);
        if (result != 0) {
            return result;
        }
        return PfUtils.compareObjects(elements, other.elements);
    }
}

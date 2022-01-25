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

package org.onap.policy.clamp.models.acm.persistence.concepts;

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
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
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
 * Class to represent a automation composition in the database.
 *
 * @author Liam Fallon (liam.fallon@est.tech)
 */
@Entity
@Table(name = "AutomationComposition")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@EqualsAndHashCode(callSuper = false)
public class JpaAutomationComposition extends PfConcept implements PfAuthorative<AutomationComposition> {
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
    private AutomationCompositionState state;

    @Column
    @NotNull
    private AutomationCompositionOrderedState orderedState;

    @Column
    private String description;

    @Column
    private Boolean primed;

    // @formatter:off
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @NotNull
    private Map<@NotNull UUID, @NotNull @Valid JpaAutomationCompositionElement> elements;
    // @formatter:on

    /**
     * The Default Constructor creates a {@link JpaAutomationComposition} object with a null key.
     */
    public JpaAutomationComposition() {
        this(new PfConceptKey());
    }

    /**
     * The Key Constructor creates a {@link JpaAutomationComposition} object with the given concept key.
     *
     * @param key the key
     */
    public JpaAutomationComposition(@NonNull final PfConceptKey key) {
        this(key, new PfConceptKey(), AutomationCompositionState.UNINITIALISED, new LinkedHashMap<>());
    }

    /**
     * The Key Constructor creates a {@link JpaAutomationComposition} object with all mandatory fields.
     *
     * @param key the key
     * @param definition the TOSCA definition of the automation composition
     * @param state the state of the automation composition
     * @param elements the elements of the automation composition in participants
     */
    public JpaAutomationComposition(@NonNull final PfConceptKey key, @NonNull final PfConceptKey definition,
        @NonNull final AutomationCompositionState state,
        @NonNull final Map<UUID, JpaAutomationCompositionElement> elements) {
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
    public JpaAutomationComposition(@NonNull final JpaAutomationComposition copyConcept) {
        super(copyConcept);
        this.key = new PfConceptKey(copyConcept.key);
        this.definition = new PfConceptKey(copyConcept.definition);
        this.state = copyConcept.state;
        this.orderedState = copyConcept.orderedState;
        this.description = copyConcept.description;
        this.elements =
            PfUtils.mapMap(copyConcept.elements, JpaAutomationCompositionElement::new, new LinkedHashMap<>(0));
        this.primed = copyConcept.primed;
    }

    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public JpaAutomationComposition(@NonNull final AutomationComposition authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
    }

    @Override
    public AutomationComposition toAuthorative() {
        var automationComposition = new AutomationComposition();

        automationComposition.setName(getKey().getName());
        automationComposition.setVersion(getKey().getVersion());
        automationComposition.setDefinition(new ToscaConceptIdentifier(definition));
        automationComposition.setState(state);
        automationComposition.setOrderedState(orderedState != null ? orderedState : state.asOrderedState());
        automationComposition.setDescription(description);
        automationComposition.setElements(
            PfUtils.mapMap(elements, JpaAutomationCompositionElement::toAuthorative, new LinkedHashMap<>(0)));
        automationComposition.setPrimed(primed);

        return automationComposition;
    }

    @Override
    public void fromAuthorative(@NonNull final AutomationComposition automationComposition) {
        if (this.key == null || this.getKey().isNullKey()) {
            this.setKey(new PfConceptKey(automationComposition.getName(), automationComposition.getVersion()));
        }

        this.definition = automationComposition.getDefinition().asConceptKey();
        this.state = automationComposition.getState();
        this.orderedState = automationComposition.getOrderedState();
        this.description = automationComposition.getDescription();
        this.primed = automationComposition.getPrimed();

        this.elements = new LinkedHashMap<>(automationComposition.getElements().size());
        for (Entry<UUID, AutomationCompositionElement> elementEntry : automationComposition.getElements().entrySet()) {
            var jpaAutomationCompositionElement = new JpaAutomationCompositionElement();
            jpaAutomationCompositionElement
                .setKey(new PfReferenceKey(getKey(), elementEntry.getValue().getId().toString()));
            jpaAutomationCompositionElement.fromAuthorative(elementEntry.getValue());
            this.elements.put(elementEntry.getKey(), jpaAutomationCompositionElement);
        }
    }

    @Override
    public List<PfKey> getKeys() {
        List<PfKey> keyList = getKey().getKeys();

        keyList.add(definition);

        for (JpaAutomationCompositionElement element : elements.values()) {
            keyList.addAll(element.getKeys());
        }

        return keyList;
    }

    @Override
    public void clean() {
        key.clean();
        definition.clean();
        description = (description == null ? null : description.trim());

        for (JpaAutomationCompositionElement element : elements.values()) {
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

        final JpaAutomationComposition other = (JpaAutomationComposition) otherConcept;
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

/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2023 Nordix Foundation.
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.base.Validated;

/**
 * Class to represent a automation composition in the database.
 *
 * @author Liam Fallon (liam.fallon@est.tech)
 */
@Entity
@Table(name = "AutomationComposition", indexes = {@Index(name = "ac_compositionId", columnList = "compositionId")})
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@EqualsAndHashCode(callSuper = false)
public class JpaAutomationComposition extends Validated
        implements PfAuthorative<AutomationComposition>, Comparable<JpaAutomationComposition> {

    @Id
    @NotNull
    private String instanceId;

    @NotNull
    @Column
    private String name;

    @NotNull
    @Column
    private String version;

    @Column
    @NotNull
    private String compositionId;

    @Column
    private Boolean restarting;

    @Column
    @NotNull
    private DeployState deployState;

    @Column
    @NotNull
    private LockState lockState;

    @Column
    private StateChangeResult stateChangeResult;

    @Column
    private String description;

    @NotNull
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "instanceId", foreignKey = @ForeignKey(name = "ac_element_fk"))
    private List<@NotNull @Valid JpaAutomationCompositionElement> elements;

    /**
     * The Default Constructor creates a {@link JpaAutomationComposition} object with a null key.
     */
    public JpaAutomationComposition() {
        this(UUID.randomUUID().toString(), new PfConceptKey(), UUID.randomUUID().toString(),
                new ArrayList<>(), DeployState.UNDEPLOYED, LockState.NONE);
    }

    /**
     * The Key Constructor creates a {@link JpaAutomationComposition} object with all mandatory fields.
     *
     * @param instanceId The UUID of the automation composition instance
     * @param key the key
     * @param compositionId the TOSCA compositionId of the automation composition definition
     * @param elements the elements of the automation composition in participants
     * @param deployState the Deploy State
     * @param lockState the Lock State
     */
    public JpaAutomationComposition(@NonNull final String instanceId, @NonNull final PfConceptKey key,
            @NonNull final String compositionId,
            @NonNull final List<JpaAutomationCompositionElement> elements,
            @NonNull final DeployState deployState, @NonNull final LockState lockState) {
        this.instanceId = instanceId;
        this.name = key.getName();
        this.version = key.getVersion();
        this.compositionId = compositionId;
        this.deployState = deployState;
        this.lockState = lockState;
        this.elements = elements;
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaAutomationComposition(@NonNull final JpaAutomationComposition copyConcept) {
        this.instanceId = copyConcept.instanceId;
        this.name = copyConcept.name;
        this.version = copyConcept.version;
        this.compositionId = copyConcept.compositionId;
        this.restarting = copyConcept.restarting;
        this.deployState = copyConcept.deployState;
        this.lockState = copyConcept.lockState;
        this.description = copyConcept.description;
        this.stateChangeResult = copyConcept.stateChangeResult;
        this.elements = PfUtils.mapList(copyConcept.elements, JpaAutomationCompositionElement::new);
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

        automationComposition.setInstanceId(UUID.fromString(instanceId));
        automationComposition.setName(name);
        automationComposition.setVersion(version);
        automationComposition.setCompositionId(UUID.fromString(compositionId));
        automationComposition.setRestarting(restarting);
        automationComposition.setDeployState(deployState);
        automationComposition.setLockState(lockState);
        automationComposition.setDescription(description);
        automationComposition.setStateChangeResult(stateChangeResult);
        automationComposition.setElements(new LinkedHashMap<>(this.elements.size()));
        for (var element : this.elements) {
            automationComposition.getElements().put(UUID.fromString(element.getElementId()), element.toAuthorative());
        }

        return automationComposition;
    }

    @Override
    public void fromAuthorative(@NonNull final AutomationComposition automationComposition) {
        this.instanceId = automationComposition.getInstanceId().toString();
        this.name = automationComposition.getName();
        this.version = automationComposition.getVersion();
        this.compositionId = automationComposition.getCompositionId().toString();
        this.restarting = automationComposition.getRestarting();
        this.deployState = automationComposition.getDeployState();
        this.lockState = automationComposition.getLockState();
        this.description = automationComposition.getDescription();
        this.stateChangeResult = automationComposition.getStateChangeResult();
        this.elements = new ArrayList<>(automationComposition.getElements().size());
        for (var elementEntry : automationComposition.getElements().entrySet()) {
            var jpaAutomationCompositionElement =
                    new JpaAutomationCompositionElement(elementEntry.getKey().toString(), this.instanceId);
            jpaAutomationCompositionElement.fromAuthorative(elementEntry.getValue());
            this.elements.add(jpaAutomationCompositionElement);
        }
    }

    @Override
    public int compareTo(final JpaAutomationComposition other) {
        if (other == null) {
            return -1;
        }
        if (this == other) {
            return 0;
        }

        var result = ObjectUtils.compare(instanceId, other.instanceId);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(name, other.name);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(version, other.version);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(compositionId, other.compositionId);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(restarting, other.restarting);
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

        result = ObjectUtils.compare(description, other.description);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(stateChangeResult, other.stateChangeResult);
        if (result != 0) {
            return result;
        }
        return PfUtils.compareObjects(elements, other.elements);
    }
}

/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2024 Nordix Foundation.
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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
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
    private String compositionTargetId;

    @Column
    private Boolean restarting;

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
    private StateChangeResult stateChangeResult;

    @Column
    @NotNull
    private Timestamp lastMsg;

    @Column
    private Integer phase;

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
        this(UUID.randomUUID().toString(), new PfConceptKey(), UUID.randomUUID().toString(), new ArrayList<>(),
                DeployState.UNDEPLOYED, LockState.NONE, SubState.NONE);
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
     * @param subState the Sub State
     */
    public JpaAutomationComposition(@NonNull final String instanceId, @NonNull final PfConceptKey key,
            @NonNull final String compositionId, @NonNull final List<JpaAutomationCompositionElement> elements,
            @NonNull final DeployState deployState, @NonNull final LockState lockState,
            @NonNull final SubState subState) {
        this.instanceId = instanceId;
        this.name = key.getName();
        this.version = key.getVersion();
        this.compositionId = compositionId;
        this.deployState = deployState;
        this.lockState = lockState;
        this.elements = elements;
        this.subState = subState;
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
        this.compositionTargetId = copyConcept.compositionTargetId;
        this.restarting = copyConcept.restarting;
        this.deployState = copyConcept.deployState;
        this.lockState = copyConcept.lockState;
        this.lastMsg = copyConcept.lastMsg;
        this.phase = copyConcept.phase;
        this.subState = copyConcept.subState;
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
        if (compositionTargetId != null) {
            automationComposition.setCompositionTargetId(UUID.fromString(compositionTargetId));
        }
        automationComposition.setRestarting(restarting);
        automationComposition.setDeployState(deployState);
        automationComposition.setLockState(lockState);
        automationComposition.setLastMsg(lastMsg.toString());
        automationComposition.setPhase(phase);
        automationComposition.setSubState(subState);
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
        this.fromAuthorativeBase(automationComposition);
        this.elements = new ArrayList<>(automationComposition.getElements().size());
        for (var elementEntry : automationComposition.getElements().entrySet()) {
            var jpaAutomationCompositionElement =
                    new JpaAutomationCompositionElement(elementEntry.getKey().toString(), this.instanceId);
            jpaAutomationCompositionElement.fromAuthorative(elementEntry.getValue());
            this.elements.add(jpaAutomationCompositionElement);
        }
    }

    /**
     * Set an instance of the persist concept to the equivalent values as the other concept without copy the elements.
     *
     * @param automationComposition the authorative concept
     */
    public void fromAuthorativeBase(@NonNull final AutomationComposition automationComposition) {
        this.instanceId = automationComposition.getInstanceId().toString();
        this.name = automationComposition.getName();
        this.version = automationComposition.getVersion();
        this.compositionId = automationComposition.getCompositionId().toString();
        if (automationComposition.getCompositionTargetId() != null) {
            this.compositionTargetId = automationComposition.getCompositionTargetId().toString();
        }
        this.restarting = automationComposition.getRestarting();
        this.deployState = automationComposition.getDeployState();
        this.lockState = automationComposition.getLockState();
        this.lastMsg = TimestampHelper.toTimestamp(automationComposition.getLastMsg());
        this.phase = automationComposition.getPhase();
        this.subState = automationComposition.getSubState();
        this.description = automationComposition.getDescription();
        this.stateChangeResult = automationComposition.getStateChangeResult();
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

        result = lastMsg.compareTo(other.lastMsg);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(phase, other.phase);
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

        result = ObjectUtils.compare(compositionTargetId, other.compositionTargetId);
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

        result = ObjectUtils.compare(subState, other.subState);
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

/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionRollback;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.base.Validated;

@Entity
@Table(name = "AutomationCompositionRollback")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@EqualsAndHashCode(callSuper = false)
public class JpaAutomationCompositionRollback extends Validated
        implements PfAuthorative<AutomationCompositionRollback>, Comparable<JpaAutomationCompositionRollback> {

    @Id
    @NotNull
    private String instanceId;

    @NotNull
    @Column
    private String compositionId;

    @NotNull
    @Valid
    @Convert(converter = StringToMapConverter.class)
    @Column(length = 100000)
    private Map<String, Object> elements = new LinkedHashMap<>();

    /**
     * The Default Constructor creates a {@link JpaAutomationComposition} object with an empty hashmap.
     */
    public JpaAutomationCompositionRollback() {
        this(UUID.randomUUID().toString(), UUID.randomUUID().toString(), new LinkedHashMap<>());
    }

    /**
     * The Key Constructor creates a {@link JpaAutomationCompositionRollback} object with all mandatory fields.
     *
     * @param instanceId The UUID of the automation composition rollback instance
     * @param compositionId the TOSCA compositionId of the automation composition rollback definition
     * @param elements the elements of the automation composition rollback
     */
    public JpaAutomationCompositionRollback(@NonNull final String instanceId, @NonNull final String compositionId,
                                         @NonNull final Map<String, Object> elements) {
        this.instanceId = instanceId;
        this.compositionId = compositionId;
        this.elements = PfUtils.mapMap(elements, UnaryOperator.identity());
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaAutomationCompositionRollback(@NonNull final JpaAutomationCompositionRollback copyConcept) {
        this.instanceId = copyConcept.instanceId;
        this.compositionId = copyConcept.compositionId;
        this.elements = PfUtils.mapMap(copyConcept.elements, UnaryOperator.identity());
    }

    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public JpaAutomationCompositionRollback(@NonNull final AutomationCompositionRollback authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
    }

    @Override
    public AutomationCompositionRollback toAuthorative() {
        var acmRollback = new AutomationCompositionRollback();
        acmRollback.setInstanceId(UUID.fromString(this.instanceId));
        acmRollback.setCompositionId(UUID.fromString(this.compositionId));
        acmRollback.setElements(this.elements.values().stream()
                .map(el -> AbstractConverter.convertObject(el, AutomationCompositionElement.class))
                .collect(Collectors.toMap(AutomationCompositionElement::getId, UnaryOperator.identity())));
        return acmRollback;
    }

    @Override
    public void fromAuthorative(@NonNull final AutomationCompositionRollback acmRollback) {
        this.instanceId = acmRollback.getInstanceId().toString();
        this.compositionId = acmRollback.getCompositionId().toString();
        this.elements = acmRollback.getElements().values().stream()
                .collect(Collectors.toMap(element -> element.getId().toString(), UnaryOperator.identity()));
    }

    @Override
    public int compareTo(final JpaAutomationCompositionRollback other) {
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

        result = ObjectUtils.compare(compositionId, other.compositionId);
        if (result != 0) {
            return result;
        }

        return PfUtils.compareObjects(elements, other.elements);
    }

}

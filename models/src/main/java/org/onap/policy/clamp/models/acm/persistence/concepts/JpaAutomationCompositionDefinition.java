/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation.
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
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.document.base.ToscaServiceTemplateValidation;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.Validated;

/**
 * Class to represent a automation composition definition in the database.
 */
@Entity
@Table(name = "AutomationCompositionDefinition")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@EqualsAndHashCode(callSuper = false)
public class JpaAutomationCompositionDefinition extends Validated
        implements PfAuthorative<AutomationCompositionDefinition> {

    @Id
    @NotNull
    private String compositionId;

    @Column
    @NotNull
    private String name;

    @Column
    @NotNull
    private String version;

    @Column
    private Boolean restarting;

    @Column
    @NotNull
    private AcTypeState state;

    @Column
    private StateChangeResult stateChangeResult;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "compositionId", foreignKey = @ForeignKey(name = "dt_element_fk"))
    private Set<JpaNodeTemplateState> elements = new HashSet<>();

    @Lob
    @Column(length = 100000)
    @Convert(converter = StringToServiceTemplateConverter.class)
    @NotNull
    @Valid
    private DocToscaServiceTemplate serviceTemplate;

    @Override
    public AutomationCompositionDefinition toAuthorative() {
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(UUID.fromString(this.compositionId));
        acmDefinition.setRestarting(this.restarting);
        acmDefinition.setState(this.state);
        acmDefinition.setStateChangeResult(this.stateChangeResult);
        acmDefinition.setServiceTemplate(this.serviceTemplate.toAuthorative());
        for (var element : this.elements) {
            var key = element.getNodeTemplateId().getName();
            acmDefinition.getElementStateMap().put(key, element.toAuthorative());
        }
        return acmDefinition;
    }

    @Override
    public void fromAuthorative(final AutomationCompositionDefinition copyConcept) {
        this.compositionId = copyConcept.getCompositionId().toString();
        this.restarting = copyConcept.getRestarting();
        this.state = copyConcept.getState();
        this.stateChangeResult = copyConcept.getStateChangeResult();
        this.serviceTemplate = new DocToscaServiceTemplate(copyConcept.getServiceTemplate());
        setName(this.serviceTemplate.getName());
        setVersion(this.serviceTemplate.getVersion());
        this.elements = new HashSet<>(copyConcept.getElementStateMap().size());
        for (var element : copyConcept.getElementStateMap().values()) {
            var nodeTemplateStateId = element.getNodeTemplateStateId().toString();
            var jpaNodeTemplateState = new JpaNodeTemplateState(nodeTemplateStateId, this.compositionId);
            jpaNodeTemplateState.fromAuthorative(element);
            this.elements.add(jpaNodeTemplateState);
        }
    }

    public JpaAutomationCompositionDefinition(final AutomationCompositionDefinition acmDefinition) {
        fromAuthorative(acmDefinition);
    }

    public JpaAutomationCompositionDefinition() {
        super();
    }

    @Override
    public BeanValidationResult validate(@NonNull String fieldName) {
        var result = super.validate(fieldName);
        if (!result.isValid()) {
            return result;
        }

        return result;
    }
}

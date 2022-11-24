/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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

import com.google.gson.annotations.SerializedName;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.Validated;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;

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

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @SerializedName("serviceTemplate")
    @Valid
    private JpaToscaServiceTemplate serviceTemplate;

    @Override
    public AutomationCompositionDefinition toAuthorative() {
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(UUID.fromString(compositionId));
        acmDefinition.setServiceTemplate(serviceTemplate.toAuthorative());
        return acmDefinition;
    }

    @Override
    public void fromAuthorative(final AutomationCompositionDefinition copyConcept) {
        compositionId = copyConcept.getCompositionId().toString();
        serviceTemplate = new JpaToscaServiceTemplate(copyConcept.getServiceTemplate());
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

        AcmUtils.validateToscaTopologyTemplate(result, serviceTemplate);

        if (!result.isValid()) {
            return result;
        }

        return result;
    }
}

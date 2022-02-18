/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.concepts;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;

/**
 * Class to represent an automation composition element definition instance.
 */
@Getter
@NoArgsConstructor
@Data
@ToString
public class AutomationCompositionElementDefinition {

    private ToscaConceptIdentifier acElementDefinitionId;

    // The definition of the Automation Composition Element in TOSCA
    private ToscaNodeTemplate automationCompositionElementToscaNodeTemplate;

    // A map indexed by the property name. Each map entry is the serialized value of the property,
    // which can be deserialized into an instance of the type of the property.
    private Map<String, ToscaProperty> commonPropertiesMap = new LinkedHashMap<>();

    /**
     * Copy constructor, does a deep copy but as all fields here are immutable, it's just a regular copy.
     *
     * @param acElementDefinition the automation composition element definition to copy from
     */
    public AutomationCompositionElementDefinition(final AutomationCompositionElementDefinition acElementDefinition) {
        this.acElementDefinitionId = acElementDefinition.acElementDefinitionId;
        this.automationCompositionElementToscaNodeTemplate =
                new ToscaNodeTemplate(acElementDefinition.automationCompositionElementToscaNodeTemplate);
        this.commonPropertiesMap = PfUtils.mapMap(acElementDefinition.commonPropertiesMap, UnaryOperator.identity());
    }
}

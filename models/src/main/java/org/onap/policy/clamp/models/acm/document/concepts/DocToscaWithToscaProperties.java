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

package org.onap.policy.clamp.models.acm.document.concepts;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.onap.policy.clamp.models.acm.document.base.DocConceptKey;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaWithToscaProperties;
import org.onap.policy.models.tosca.utils.ToscaUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DocToscaWithToscaProperties<T extends ToscaWithToscaProperties> extends DocToscaEntity<T> {

    private static final long serialVersionUID = 1L;

    private Map<@NotNull @NotBlank String, @NotNull @Valid DocToscaProperty> properties;

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaWithToscaProperties(DocToscaWithToscaProperties<T> copyConcept) {
        super(copyConcept);
        this.properties = PfUtils.mapMap(copyConcept.properties, DocToscaProperty::new);
    }

    @Override
    public T toAuthorative() {
        var tosca = super.toAuthorative();
        tosca.setProperties(PfUtils.mapMap(properties, DocToscaProperty::toAuthorative));
        return tosca;
    }

    /**
     * Validates the fields of the object, including its key.
     *
     * @param fieldName name of the field containing this
     * @return the result, or {@code null}
     */
    protected BeanValidationResult validateWithKey(@NonNull String fieldName) {
        var result = super.validate(fieldName);

        validateKeyVersionNotNull(result, "key", getConceptKey());

        return result;
    }

    @Override
    public void fromAuthorative(T authorativeConcept) {
        super.fromAuthorative(authorativeConcept);

        // Set properties
        if (authorativeConcept.getProperties() != null) {
            properties = new LinkedHashMap<>();
            for (var toscaPropertyEntry : authorativeConcept.getProperties().entrySet()) {
                var jpaProperty = new DocToscaProperty(toscaPropertyEntry.getValue());
                jpaProperty.setName(toscaPropertyEntry.getKey());
                properties.put(toscaPropertyEntry.getKey(), jpaProperty);
            }
        }
    }

    /**
     * Get the referenced data types.
     *
     * @return the referenced data types
     */
    public Collection<DocConceptKey> getReferencedDataTypes() {
        if (properties == null) {
            return CollectionUtils.emptyCollection();
        }

        Set<DocConceptKey> referencedDataTypes = new LinkedHashSet<>();

        for (var property : properties.values()) {
            referencedDataTypes.add(property.getTypeDocConceptKey());

            if (property.getEntrySchema() != null) {
                referencedDataTypes.add(property.getEntrySchema().getTypeDocConceptKey());
            }
        }

        var set = ToscaUtils.getPredefinedDataTypes().stream().map(DocConceptKey::new).collect(Collectors.toSet());
        referencedDataTypes.removeAll(set);
        return referencedDataTypes;
    }
}

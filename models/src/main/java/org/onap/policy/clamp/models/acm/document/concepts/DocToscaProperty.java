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

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.document.base.DocConceptKey;
import org.onap.policy.clamp.models.acm.document.base.DocUtil;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;

@Data
@NoArgsConstructor
public class DocToscaProperty implements PfAuthorative<ToscaProperty>, Serializable, Comparable<DocToscaProperty> {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String name;

    @NotNull
    private String type;

    @SerializedName("type_version")
    @NotNull
    private String typeVersion;

    @NotBlank
    private String description;

    @SerializedName("default")
    @NotBlank
    private Object defaultValue;

    private boolean required = false;
    private ToscaProperty.Status status;
    private List<@NotNull @Valid DocToscaConstraint> constraints;

    @SerializedName("key_schema")
    @Valid
    private DocToscaSchemaDefinition keySchema;

    @SerializedName("entry_schema")
    @Valid
    private DocToscaSchemaDefinition entrySchema;

    private Map<String, String> metadata;

    public DocToscaProperty(ToscaProperty toscaProperty) {
        fromAuthorative(toscaProperty);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaProperty(final DocToscaProperty copyConcept) {
        this.name = copyConcept.name;
        this.type = copyConcept.type;
        this.typeVersion = copyConcept.typeVersion;
        this.description = copyConcept.description;
        this.defaultValue = copyConcept.defaultValue;
        this.required = copyConcept.required;
        this.status = copyConcept.status;
        this.constraints = PfUtils.mapList(copyConcept.constraints, DocToscaConstraint::new);
        if (copyConcept.keySchema != null) {
            this.keySchema = new DocToscaSchemaDefinition(copyConcept.keySchema);
        }
        if (copyConcept.entrySchema != null) {
            this.entrySchema = new DocToscaSchemaDefinition(copyConcept.entrySchema);
        }
        this.metadata = (copyConcept.metadata != null ? new LinkedHashMap<>(copyConcept.metadata) : null);
    }

    @Override
    public ToscaProperty toAuthorative() {
        var toscaProperty = new ToscaProperty();

        toscaProperty.setName(name);
        toscaProperty.setType(type);
        toscaProperty.setTypeVersion(typeVersion);
        toscaProperty.setDescription(description);
        toscaProperty.setRequired(required);
        toscaProperty.setStatus(status);
        toscaProperty.setDefaultValue(defaultValue);
        toscaProperty.setConstraints(PfUtils.mapList(constraints, DocToscaConstraint::toAuthorative));

        if (entrySchema != null) {
            toscaProperty.setEntrySchema(entrySchema.toAuthorative());
        }
        if (keySchema != null) {
            toscaProperty.setEntrySchema(keySchema.toAuthorative());
        }

        toscaProperty.setMetadata(PfUtils.mapMap(metadata, metadataItem -> metadataItem));

        return toscaProperty;
    }

    @Override
    public void fromAuthorative(ToscaProperty toscaProperty) {
        name = toscaProperty.getName();

        var key = DocUtil.createDocConceptKey(toscaProperty.getType(), toscaProperty.getTypeVersion());
        type = key.getName();
        typeVersion = key.getVersion();

        description = toscaProperty.getDescription();
        required = toscaProperty.isRequired();
        status = toscaProperty.getStatus();
        defaultValue = toscaProperty.getDefaultValue();
        constraints = PfUtils.mapList(toscaProperty.getConstraints(), DocToscaConstraint::new);

        if (toscaProperty.getEntrySchema() != null) {
            entrySchema = new DocToscaSchemaDefinition(toscaProperty.getEntrySchema());
        }
        if (toscaProperty.getKeySchema() != null) {
            keySchema = new DocToscaSchemaDefinition(toscaProperty.getKeySchema());
        }

        metadata = PfUtils.mapMap(toscaProperty.getMetadata(), metadataItem -> metadataItem);
    }

    public DocConceptKey getTypeDocConceptKey() {
        return new DocConceptKey(type, typeVersion);
    }

    @Override
    public int compareTo(DocToscaProperty otherConcept) {
        if (otherConcept == null) {
            return -1;
        }
        if (this == otherConcept) {
            return 0;
        }

        int result = name.compareTo(otherConcept.name);
        if (result != 0) {
            return result;
        }

        return compareFields(otherConcept);
    }

    /**
     * Compare the fields of this ToscaProperty object with the fields of the other ToscaProperty object.
     *
     * @param other the other ToscaProperty object
     */
    private int compareFields(final DocToscaProperty other) {
        if (!type.equals(other.type)) {
            return type.compareTo(other.type);
        }

        int result = ObjectUtils.compare(description, other.description);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(required, other.required);
        if (result != 0) {
            return result;
        }

        result = PfUtils.compareObjects(defaultValue, other.defaultValue);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(status, other.status);
        if (result != 0) {
            return result;
        }

        result = PfUtils.compareCollections(constraints, other.constraints);
        if (result != 0) {
            return result;
        }

        result = entrySchema.compareTo(other.entrySchema);
        if (result != 0) {
            return result;
        }

        return PfUtils.compareMaps(metadata, other.metadata);
    }
}

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
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.document.base.DocConceptKey;
import org.onap.policy.clamp.models.acm.document.base.DocUtil;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaSchemaDefinition;

@Data
@NoArgsConstructor
public class DocToscaSchemaDefinition
        implements PfAuthorative<ToscaSchemaDefinition>, Serializable, Comparable<DocToscaSchemaDefinition> {

    private static final long serialVersionUID = 1L;

    private String name;
    private String type;

    @SerializedName("type_version")
    private String typeVersion;

    private String description;
    private List<DocToscaConstraint> constraints;

    public DocToscaSchemaDefinition(ToscaSchemaDefinition toscaEntrySchema) {
        fromAuthorative(toscaEntrySchema);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaSchemaDefinition(final DocToscaSchemaDefinition copyConcept) {
        this.name = copyConcept.name;
        this.type = copyConcept.type;
        this.typeVersion = copyConcept.typeVersion;
        this.description = copyConcept.description;
        this.constraints = PfUtils.mapList(copyConcept.constraints, DocToscaConstraint::new);
    }

    @Override
    public ToscaSchemaDefinition toAuthorative() {
        var toscaEntrySchema = new ToscaSchemaDefinition();

        toscaEntrySchema.setName(name);
        toscaEntrySchema.setType(getTypeDocConceptKey().getId());
        toscaEntrySchema.setDescription(description);

        if (constraints != null) {
            toscaEntrySchema.setConstraints(PfUtils.mapList(constraints, DocToscaConstraint::toAuthorative));
        }

        return toscaEntrySchema;
    }

    @Override
    public void fromAuthorative(ToscaSchemaDefinition toscaEntrySchema) {
        name = toscaEntrySchema.getName();

        var key = DocUtil.createDocConceptKey(toscaEntrySchema.getType(), toscaEntrySchema.getTypeVersion());
        type = key.getName();
        typeVersion = key.getVersion();

        description = toscaEntrySchema.getDescription();

        if (toscaEntrySchema.getConstraints() != null) {
            constraints = PfUtils.mapList(toscaEntrySchema.getConstraints(), DocToscaConstraint::new);

        }
    }

    public DocConceptKey getTypeDocConceptKey() {
        return new DocConceptKey(type, typeVersion);
    }

    @Override
    public int compareTo(DocToscaSchemaDefinition other) {
        if (other == null) {
            return -1;
        }
        if (this == other) {
            return 0;
        }

        int result = ObjectUtils.compare(description, other.description);
        if (result != 0) {
            return result;
        }

        return PfUtils.compareCollections(constraints, other.constraints);
    }
}

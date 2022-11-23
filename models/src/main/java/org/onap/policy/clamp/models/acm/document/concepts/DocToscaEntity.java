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
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.document.base.DocConceptKey;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfNameVersion;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.base.Validated;
import org.onap.policy.models.tosca.authorative.concepts.ToscaEntity;
import org.onap.policy.models.tosca.authorative.concepts.ToscaEntityKey;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DocToscaEntity<T extends ToscaEntity> extends Validated
        implements PfNameVersion, PfAuthorative<T>, Serializable, Comparable<DocToscaEntity<T>> {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String name = PfKey.NULL_KEY_NAME;

    @NotNull
    private String version = PfKey.NULL_KEY_VERSION;

    @SerializedName("derived_from")
    private String derivedFrom;

    private Map<@NotNull @NotBlank String, @NotNull @NotBlank Object> metadata = new LinkedHashMap<>();

    @NotBlank
    private String description;

    private transient T toscaEntity;

    /**
     * Get a key for this entity.
     *
     * @return a ToscaEntityKey for this entry
     */
    public ToscaEntityKey getKey() {
        return new ToscaEntityKey(name, version);
    }

    /**
     * Get a key for this entity.
     *
     * @return a PfConceptKey for this entry
     */
    public PfConceptKey getConceptKey() {
        return new PfConceptKey(name, version);
    }

    public DocConceptKey getDocConceptKey() {
        return new DocConceptKey(name, version);
    }

    @Override
    public String getDefinedName() {
        return (PfKey.NULL_KEY_NAME.equals(name) ? null : name);
    }

    @Override
    public String getDefinedVersion() {
        return (PfKey.NULL_KEY_VERSION.equals(version) ? null : version);
    }

    /**
     * Method that should be specialised to return the type of the entity if the entity has a type.
     *
     * @return the type of the entity or null if it has no type
     */
    public String getType() {
        return null;
    }

    /**
     * Method that should be specialised to return the type version of the entity if the entity has a type.
     *
     * @return the type of the entity or null if it has no type
     */
    public String getTypeVersion() {
        return null;
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaEntity(final DocToscaEntity<T> copyConcept) {
        this.name = copyConcept.name;
        this.version = copyConcept.version;
        this.derivedFrom = copyConcept.derivedFrom;
        this.metadata = (copyConcept.metadata != null ? new LinkedHashMap<>(copyConcept.metadata) : null);
        this.description = copyConcept.description;
    }

    @Override
    public T toAuthorative() {
        toscaEntity.setName(getName());
        toscaEntity.setVersion(getVersion());

        if (derivedFrom != null) {
            toscaEntity.setDerivedFrom(derivedFrom);
        }

        if (description != null) {
            toscaEntity.setDescription(description);
        }

        toscaEntity.setMetadata(metadata);

        return toscaEntity;
    }

    @Override
    public void fromAuthorative(T toscaEntity) {
        if (toscaEntity.getName() != null) {
            name = toscaEntity.getName();
        }
        if (toscaEntity.getVersion() != null) {
            version = toscaEntity.getVersion();
        }
        if (toscaEntity.getDerivedFrom() != null) {
            derivedFrom = toscaEntity.getDerivedFrom();
        }

        if (toscaEntity.getDescription() != null) {
            description = toscaEntity.getDescription();
        }

        metadata = toscaEntity.getMetadata();
    }

    @Override
    public int compareTo(final DocToscaEntity<T> otherConcept) {
        if (otherConcept == null) {
            return -1;
        }
        if (this == otherConcept) {
            return 0;
        }
        if (getClass() != otherConcept.getClass()) {
            return getClass().getName().compareTo(otherConcept.getClass().getName());
        }

        int result = getKey().asIdentifier().compareTo(otherConcept.getKey().asIdentifier());
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(derivedFrom, otherConcept.derivedFrom);
        if (result != 0) {
            return result;
        }

        result = PfUtils.compareMaps(metadata, otherConcept.metadata);
        if (result != 0) {
            return result;
        }

        return ObjectUtils.compare(description, otherConcept.description);
    }
}

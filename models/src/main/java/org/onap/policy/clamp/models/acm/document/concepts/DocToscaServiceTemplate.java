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
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.document.base.DocUtil;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocToscaServiceTemplate extends DocToscaEntity<ToscaServiceTemplate> {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_TOSCA_DEFINTIONS_VERISON = "tosca_simple_yaml_1_1_0";
    public static final String DEFAULT_NAME = "tosca";
    public static final String DEFAULT_VERSION = "1.0.0";

    @SerializedName("tosca_definitions_version")
    @NotNull
    @NotBlank
    private String toscaDefinitionsVersion;

    @SerializedName("data_types")
    private Map<String, @Valid DocToscaDataType> dataTypes;

    @SerializedName("capability_types")
    private Map<String, @Valid DocToscaCapabilityType> capabilityTypes;

    @SerializedName("node_types")
    private Map<String, @Valid DocToscaNodeType> nodeTypes;

    @SerializedName("relationship_types")
    private Map<String, @Valid DocToscaRelationshipType> relationshipTypes;

    @SerializedName("policy_types")
    private Map<String, @Valid DocToscaPolicyType> policyTypes;

    @SerializedName("topology_template")
    @Valid
    private DocToscaTopologyTemplate toscaTopologyTemplate;

    public DocToscaServiceTemplate(ToscaServiceTemplate authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
    }

    /**
     * The Default Constructor creates a {@link DocToscaServiceTemplate} object with a null key.
     */
    public DocToscaServiceTemplate() {
        super();
        setName(DEFAULT_NAME);
        setVersion(DEFAULT_VERSION);
        setToscaDefinitionsVersion(DEFAULT_TOSCA_DEFINTIONS_VERISON);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaServiceTemplate(final DocToscaServiceTemplate copyConcept) {
        super(copyConcept);
        this.toscaDefinitionsVersion = copyConcept.toscaDefinitionsVersion;
        this.dataTypes = PfUtils.mapMap(copyConcept.dataTypes, DocToscaDataType::new, new LinkedHashMap<>());
        this.capabilityTypes =
                PfUtils.mapMap(copyConcept.capabilityTypes, DocToscaCapabilityType::new, new LinkedHashMap<>());
        this.nodeTypes = PfUtils.mapMap(copyConcept.nodeTypes, DocToscaNodeType::new, new LinkedHashMap<>());
        this.relationshipTypes = PfUtils.mapMap(copyConcept.relationshipTypes, DocToscaRelationshipType::new,
                new LinkedHashMap<>());
        this.policyTypes = PfUtils.mapMap(copyConcept.policyTypes, DocToscaPolicyType::new, new LinkedHashMap<>());
        if (copyConcept.toscaTopologyTemplate != null) {
            this.toscaTopologyTemplate = new DocToscaTopologyTemplate(copyConcept.toscaTopologyTemplate);
        }
    }

    @Override
    public ToscaServiceTemplate toAuthorative() {
        final var toscaServiceTemplate = new ToscaServiceTemplate();
        super.setToscaEntity(toscaServiceTemplate);
        super.toAuthorative();

        toscaServiceTemplate.setToscaDefinitionsVersion(toscaDefinitionsVersion);
        toscaServiceTemplate.setDataTypes(DocUtil.docMapToMap(dataTypes, DocToscaDataType::toAuthorative));
        toscaServiceTemplate
                .setCapabilityTypes(DocUtil.docMapToMap(capabilityTypes, DocToscaCapabilityType::toAuthorative));
        toscaServiceTemplate
                .setRelationshipTypes(DocUtil.docMapToMap(relationshipTypes, DocToscaRelationshipType::toAuthorative));
        toscaServiceTemplate.setNodeTypes(DocUtil.docMapToMap(nodeTypes, DocToscaNodeType::toAuthorative));
        toscaServiceTemplate.setPolicyTypes(DocUtil.docMapToMap(policyTypes, DocToscaPolicyType::toAuthorative));
        toscaServiceTemplate.setToscaTopologyTemplate(toscaTopologyTemplate.toAuthorative());

        return toscaServiceTemplate;
    }

    @Override
    public void fromAuthorative(ToscaServiceTemplate toscaServiceTemplate) {
        super.fromAuthorative(toscaServiceTemplate);
        if (getVersion() == null || PfKey.NULL_KEY_VERSION.equals(getVersion())) {
            setVersion(DEFAULT_VERSION);
        }
        if (getName() == null || PfKey.NULL_KEY_NAME.equals(getName())) {
            setName(DEFAULT_NAME);
        }

        toscaDefinitionsVersion = toscaServiceTemplate.getToscaDefinitionsVersion();

        dataTypes = DocUtil.mapToDocMap(toscaServiceTemplate.getDataTypes(), DocToscaDataType::new);

        capabilityTypes = DocUtil.mapToDocMap(toscaServiceTemplate.getCapabilityTypes(), DocToscaCapabilityType::new);

        relationshipTypes =
                DocUtil.mapToDocMap(toscaServiceTemplate.getRelationshipTypes(), DocToscaRelationshipType::new);

        nodeTypes = DocUtil.mapToDocMap(toscaServiceTemplate.getNodeTypes(), DocToscaNodeType::new);

        if (toscaServiceTemplate.getPolicyTypes() != null) {
            policyTypes = DocUtil.mapToDocMap(toscaServiceTemplate.getPolicyTypes(), DocToscaPolicyType::new);
        }

        if (toscaServiceTemplate.getToscaTopologyTemplate() != null) {
            toscaTopologyTemplate = new DocToscaTopologyTemplate(toscaServiceTemplate.getToscaTopologyTemplate());
        }
    }

    @Override
    public int compareTo(DocToscaEntity<ToscaServiceTemplate> otherConcept) {
        int result = compareToWithoutEntities(otherConcept);
        if (result != 0) {
            return result;
        }

        final var other = (DocToscaServiceTemplate) otherConcept;

        result = DocUtil.compareMaps(dataTypes, other.dataTypes);
        if (result != 0) {
            return result;
        }

        result = DocUtil.compareMaps(capabilityTypes, other.capabilityTypes);
        if (result != 0) {
            return result;
        }

        result = DocUtil.compareMaps(relationshipTypes, other.relationshipTypes);
        if (result != 0) {
            return result;
        }

        result = DocUtil.compareMaps(nodeTypes, other.nodeTypes);
        if (result != 0) {
            return result;
        }

        result = DocUtil.compareMaps(policyTypes, other.policyTypes);
        if (result != 0) {
            return result;
        }

        return ObjectUtils.compare(toscaTopologyTemplate, other.toscaTopologyTemplate);
    }

    /**
     * Compare this service template to another service template, ignoring contained entitites.
     *
     * @param otherConcept the other topology template
     * @return the result of the comparison
     */
    public int compareToWithoutEntities(final DocToscaEntity<ToscaServiceTemplate> otherConcept) {
        if (otherConcept == null) {
            return -1;
        }
        if (this == otherConcept) {
            return 0;
        }
        if (getClass() != otherConcept.getClass()) {
            return getClass().getName().compareTo(otherConcept.getClass().getName());
        }

        final var other = (DocToscaServiceTemplate) otherConcept;
        if (!super.equals(other)) {
            return super.compareTo(other);
        }

        return ObjectUtils.compare(toscaDefinitionsVersion, other.toscaDefinitionsVersion);
    }
}

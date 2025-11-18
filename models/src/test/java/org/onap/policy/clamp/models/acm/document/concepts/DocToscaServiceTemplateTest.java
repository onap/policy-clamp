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

package org.onap.policy.clamp.models.acm.document.concepts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.document.base.DocConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaCapabilityType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConstraint;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaRelationshipType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaRequirement;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;

class DocToscaServiceTemplateTest {

    private static final String NAME = "name";
    private static final String NAME_VERSION = "name:0.0.0";

    @Test
    void test() {
        var serviceTemplate = new ToscaServiceTemplate();
        serviceTemplate.setName(PfKey.NULL_KEY_NAME);
        serviceTemplate.setVersion(PfKey.NULL_KEY_VERSION);
        var doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc.toAuthorative()).isNotNull();
        assertThat(doc).isNotEqualByComparingTo(null)
                .isEqualByComparingTo(doc);

        var doc2 = new DocToscaServiceTemplate(doc);
        assertThat(doc2.toAuthorative()).isNotNull();
        assertThat(doc).isEqualByComparingTo(doc2);

        serviceTemplate.setToscaTopologyTemplate(new ToscaTopologyTemplate());
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setToscaTopologyTemplate(doc.getToscaTopologyTemplate());
        assertThat(doc).isEqualByComparingTo(doc2);

        serviceTemplate.setDataTypes(Map.of(NAME, new ToscaDataType()));
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setDataTypes(doc.getDataTypes());
        assertThat(doc).isEqualByComparingTo(doc2);

        serviceTemplate.setCapabilityTypes(Map.of(NAME, new ToscaCapabilityType()));
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setCapabilityTypes(doc.getCapabilityTypes());
        assertThat(doc).isEqualByComparingTo(doc2);

        serviceTemplate.setRelationshipTypes(Map.of(NAME, new ToscaRelationshipType()));
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setRelationshipTypes(doc.getRelationshipTypes());
        assertThat(doc).isEqualByComparingTo(doc2);

        serviceTemplate.setNodeTypes(Map.of(NAME, new ToscaNodeType()));
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setNodeTypes(doc.getNodeTypes());
        assertThat(doc).isEqualByComparingTo(doc2);

        serviceTemplate.setPolicyTypes(Map.of(NAME, new ToscaPolicyType()));
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setPolicyTypes(doc.getPolicyTypes());
        assertThat(doc).isEqualByComparingTo(doc2);
        assertThat(doc.toAuthorative()).isEqualTo(doc2.toAuthorative());
        assertThat(doc.validate("serviceTemplate")).isNotNull();

        serviceTemplate.setName(NAME);
        serviceTemplate.setVersion("1.1.1");
        doc.fromAuthorative(serviceTemplate);
        assertThat(doc.toAuthorative()).isNotNull();
    }

    @Test
    void testToscaDataType() {
        var serviceTemplate = new ToscaServiceTemplate();
        var dataType = new ToscaDataType();
        serviceTemplate.setDataTypes(Map.of(NAME, dataType));
        var toscaConstraint = new ToscaConstraint();
        toscaConstraint.setRangeValues(List.of("value"));
        toscaConstraint.setValidValues(List.of("value"));
        var doc = new DocToscaServiceTemplate(serviceTemplate);
        var doc2 = new DocToscaServiceTemplate(doc);

        dataType.setConstraints(List.of(toscaConstraint));
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setDataTypes(Map.of(NAME_VERSION, doc.getDataTypes().get(NAME_VERSION)));
        assertThat(doc).isEqualByComparingTo(doc2);
        assertThat(doc.toAuthorative()).isEqualTo(doc2.toAuthorative());

        var property = new ToscaProperty();
        property.setType("type");
        dataType.setProperties(Map.of(NAME, property));
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
    }

    @Test
    void testDocToscaEntity() {
        var serviceTemplate = new ToscaServiceTemplate();
        var doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc.getType()).isNull();
        assertThat(doc.getTypeVersion()).isNull();
        assertThat(doc.getDefinedName()).isEqualTo("tosca");
        assertThat(doc.getDefinedVersion()).isEqualTo("1.0.0");

        serviceTemplate.setDerivedFrom(NAME);
        var doc2 = new DocToscaServiceTemplate(doc);
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setDerivedFrom(doc.getDerivedFrom());
        assertThat(doc).isEqualByComparingTo(doc2);

        serviceTemplate.setDescription("Description");
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setDescription(doc.getDescription());
        assertThat(doc).isEqualByComparingTo(doc2);

        serviceTemplate.setMetadata(Map.of("key", "value"));
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setMetadata(doc.getMetadata());
        assertThat(doc).isEqualByComparingTo(doc2);

        var d = new DocToscaEntity<ToscaServiceTemplate>();
        var result = doc.compareTo(d);
        assertThat(result).isNotZero();
    }

    @Test
    void testDocToscaNodeType() {
        var serviceTemplate = new ToscaServiceTemplate();
        var nodeType = new ToscaNodeType();
        serviceTemplate.setNodeTypes(Map.of(NAME, nodeType));
        var doc = new DocToscaServiceTemplate(serviceTemplate);
        var doc2 = new DocToscaServiceTemplate(doc);
        var docNodeType = doc.getNodeTypes().get(NAME_VERSION);
        doc2.setNodeTypes(Map.of(NAME_VERSION, docNodeType));
        assertThat(doc).isEqualByComparingTo(doc2);

        var requirement = new ToscaRequirement();
        requirement.setType(NAME);
        requirement.setTypeVersion("1.0.0");
        nodeType.setRequirements(List.of(Map.of(NAME, requirement)));
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        docNodeType = doc.getNodeTypes().get(NAME_VERSION);
        doc2.getNodeTypes().get(NAME_VERSION).setRequirements(docNodeType.getRequirements());
        assertThat(doc).isEqualByComparingTo(doc2);

        var docRequirement = docNodeType.getRequirements().get(0).get(NAME_VERSION);
        assertThat(docRequirement)
                .isNotEqualByComparingTo(null)
                .isEqualByComparingTo(docRequirement);

        assertThat(doc.toAuthorative()).isEqualTo(doc2.toAuthorative());
        doc2 = new DocToscaServiceTemplate(doc);
        assertThat(doc.toAuthorative()).isEqualTo(doc2.toAuthorative());
    }

    @Test
    void testDocToscaProperty() {
        var serviceTemplate = new ToscaServiceTemplate();
        var property = new ToscaProperty();
        property.setType("type");
        var nodeType = new ToscaNodeType();
        nodeType.setProperties(Map.of(NAME, property));
        serviceTemplate.setNodeTypes(Map.of(NAME, nodeType));
        var doc = new DocToscaServiceTemplate(serviceTemplate);
        var doc2 = new DocToscaServiceTemplate(doc);

        property.setDescription("Description");
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        var docNodeType = doc2.getNodeTypes().get(NAME_VERSION);
        var docProperty = docNodeType.getProperties().get(NAME);
        docProperty.setDescription(property.getDescription());
        assertThat(doc).isEqualByComparingTo(doc2);

        property.setRequired(true);
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        docProperty.setRequired(property.isRequired());
        assertThat(doc).isEqualByComparingTo(doc2);

        property.setDefaultValue("Default");
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        docProperty.setDefaultValue(property.getDefaultValue());
        assertThat(doc).isEqualByComparingTo(doc2);

        property.setStatus(ToscaProperty.Status.SUPPORTED);
        doc = new DocToscaServiceTemplate(serviceTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        docProperty.setStatus(property.getStatus());
        assertThat(doc).isEqualByComparingTo(doc2);
    }

    @Test
    void testNullPointer() {
        var docCapabilityType = new DocToscaCapabilityType();
        assertThatThrownBy(() -> docCapabilityType.validate(null)).   isInstanceOf(NullPointerException.class);

        var docConceptKey = new DocConceptKey();
        assertThatThrownBy(() -> docConceptKey.setName(null)).   isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> docConceptKey.setVersion(null)).   isInstanceOf(NullPointerException.class);
    }
}

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

package org.onap.policy.clamp.models.acm.document.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaDataType;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaNodeTemplate;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaPolicy;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaProperty;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaRequirement;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaTopologyTemplate;
import org.onap.policy.common.parameters.BeanValidationResult;

class ToscaServiceTemplateValidationTest {

    public static final String AUTOMATION_COMPOSITION_ELEMENT =
            "org.onap.policy.clamp.acm.AutomationCompositionElement";
    public static final String AUTOMATION_COMPOSITION_NODE_TYPE = "org.onap.policy.clamp.acm.AutomationComposition";
    private static final String NAME_VERSION1 = "name:1.0.0";
    private static final String NAME_VERSION2 = "name:1.0.1";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String VERSION = "1.0.0";

    @Test
    void testValidate() {
        var doc = new DocToscaServiceTemplate();
        assertThatThrownBy(
                () -> ToscaServiceTemplateValidation.validate(null, doc, AUTOMATION_COMPOSITION_NODE_TYPE))
                .isInstanceOf(NullPointerException.class);
        var result = getValidate(doc);
        assertThat(result.isValid()).isFalse();

        var docDataType = new DocToscaDataType();
        docDataType.setName(NAME);
        docDataType.setVersion(VERSION);
        var docProperty = new DocToscaProperty();
        docProperty.setType(TYPE);
        docProperty.setTypeVersion(VERSION);
        docDataType.setProperties(Map.of(NAME_VERSION1, docProperty));
        docDataType.setDerivedFrom("NotExist");
        doc.setDataTypes(Map.of(NAME_VERSION1, docDataType));
        result = getValidate(doc);
        assertThat(result.isValid()).isFalse();

        // ancestor of itself
        docDataType.setDerivedFrom(NAME);
        result = getValidate(doc);
        assertThat(result.isValid()).isFalse();

        doc.setDataTypes(null);
        var docTopologyTemplate = new DocToscaTopologyTemplate();
        doc.setToscaTopologyTemplate(docTopologyTemplate);
        result = getValidate(doc);
        assertThat(result.isValid()).isFalse();

        var docPolicy = new DocToscaPolicy();
        docPolicy.setType(TYPE);
        docPolicy.setTypeVersion(VERSION);
        docTopologyTemplate.setPolicies(Map.of(NAME_VERSION1, docPolicy));
        result = getValidate(doc);
        assertThat(result.isValid()).isFalse();

        docTopologyTemplate.setPolicies(null);
        var docNodeTemplates = new DocToscaNodeTemplate();
        docNodeTemplates.setType(TYPE);
        docNodeTemplates.setTypeVersion(VERSION);
        var docRequirement = new DocToscaRequirement();
        docRequirement.setType(TYPE);
        docRequirement.setTypeVersion(VERSION);
        docNodeTemplates.setRequirements(List.of(Map.of(NAME_VERSION1, docRequirement)));
        docTopologyTemplate.setNodeTemplates(Map.of(NAME_VERSION1, docNodeTemplates));
        result = getValidate(doc);
        assertThat(result.isValid()).isFalse();

        docRequirement.setDerivedFrom("NotExist");
        result = getValidate(doc);
        assertThat(result.isValid()).isFalse();
    }

    private BeanValidationResult getValidate(DocToscaServiceTemplate doc) {
        var result = new BeanValidationResult("DocToscaServiceTemplate", doc);
        ToscaServiceTemplateValidation.validate(result, doc, AUTOMATION_COMPOSITION_NODE_TYPE);
        return result;
    }

    @Test
    void testValidateToscaTopologyTemplate() {
        var result = getValidateToscaTopologyTemplate(null);
        assertThat(result.isValid()).isFalse();

        var doc = new DocToscaTopologyTemplate();
        assertThatThrownBy(() -> ToscaServiceTemplateValidation
                .validateToscaTopologyTemplate(null, doc, AUTOMATION_COMPOSITION_NODE_TYPE))
                .isInstanceOf(NullPointerException.class);
        result = getValidateToscaTopologyTemplate(doc);
        assertThat(result.isValid()).isFalse();

        var docNodeTemplate1 = new DocToscaNodeTemplate();
        docNodeTemplate1.setType(AUTOMATION_COMPOSITION_NODE_TYPE);
        docNodeTemplate1.setTypeVersion(VERSION);
        var docNodeTemplate2 = new DocToscaNodeTemplate();
        docNodeTemplate2.setType(AUTOMATION_COMPOSITION_NODE_TYPE);
        docNodeTemplate2.setTypeVersion("1.0.1");
        doc.setNodeTemplates(Map.of(NAME_VERSION1, docNodeTemplate1, NAME_VERSION2, docNodeTemplate2));
        result = getValidateToscaTopologyTemplate(doc);
        assertThat(result.isValid()).isFalse();
    }

    private BeanValidationResult getValidateToscaTopologyTemplate(DocToscaTopologyTemplate doc) {
        var result = new BeanValidationResult("DocToscaTopologyTemplate", doc);
        ToscaServiceTemplateValidation.validateToscaTopologyTemplate(result, doc, AUTOMATION_COMPOSITION_NODE_TYPE);
        return result;
    }
}

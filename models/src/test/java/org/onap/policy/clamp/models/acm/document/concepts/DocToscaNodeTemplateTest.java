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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaCapabilityAssignment;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaRequirement;

class DocToscaNodeTemplateTest {

    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String VERSION = "1.0.0";
    private static final String NAME_VERSION = "name:0.0.0";

    @Test
    void testToscaNodeTemplate() {
        var nodeTemplate = new ToscaNodeTemplate();
        nodeTemplate.setType(TYPE);
        nodeTemplate.setTypeVersion(VERSION);
        var doc = new DocToscaNodeTemplate(nodeTemplate);
        assertThat(doc)
                .isNotEqualByComparingTo(null)
                .isEqualByComparingTo(doc);

        var requirement = new ToscaRequirement();
        requirement.setType(TYPE);
        requirement.setTypeVersion(VERSION);
        nodeTemplate.setRequirements(List.of(Map.of(NAME, requirement)));
        var doc2 = new DocToscaNodeTemplate(doc);
        doc = new DocToscaNodeTemplate(nodeTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setRequirements(doc.getRequirements());
        assertThat(doc).isEqualByComparingTo(doc2);

        var capabilityAssignment = new ToscaCapabilityAssignment();
        capabilityAssignment.setType(TYPE);
        capabilityAssignment.setTypeVersion(VERSION);
        nodeTemplate.setCapabilities(Map.of(NAME, capabilityAssignment));
        doc = new DocToscaNodeTemplate(nodeTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setCapabilities(doc.getCapabilities());
        assertThat(doc).isEqualByComparingTo(doc2);

        capabilityAssignment.setAttributes(Map.of("key", "value"));
        doc = new DocToscaNodeTemplate(nodeTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        var docCapabilityAssignment = doc.getCapabilities().get(NAME_VERSION);
        assertThat(docCapabilityAssignment)
                .isNotEqualByComparingTo(null)
                .isEqualByComparingTo(docCapabilityAssignment);

        doc2.getCapabilities().get(NAME_VERSION).setAttributes(docCapabilityAssignment.getAttributes());
        assertThat(doc).isEqualByComparingTo(doc2);

        assertThat(doc.toAuthorative()).isEqualTo(doc2.toAuthorative());
        doc2 = new DocToscaNodeTemplate(doc);
        assertThat(doc.toAuthorative()).isEqualTo(doc2.toAuthorative());

        capabilityAssignment.setAttributes(null);
        capabilityAssignment.setOccurrences(List.of(NAME));
        doc = new DocToscaNodeTemplate(nodeTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        docCapabilityAssignment = doc.getCapabilities().get(NAME_VERSION);

        doc2.getCapabilities().get(NAME_VERSION).setAttributes(docCapabilityAssignment.getAttributes());
        doc2.getCapabilities().get(NAME_VERSION).setOccurrences(docCapabilityAssignment.getOccurrences());
        assertThat(doc).isEqualByComparingTo(doc2);

        assertThat(doc.toAuthorative()).isEqualTo(doc2.toAuthorative());
        doc2 = new DocToscaNodeTemplate(doc);
        assertThat(doc.toAuthorative()).isEqualTo(doc2.toAuthorative());
    }
}

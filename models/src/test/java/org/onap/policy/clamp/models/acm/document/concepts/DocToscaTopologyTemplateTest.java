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
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaParameter;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;

class DocToscaTopologyTemplateTest {

    private static final String NAME = "name";

    @Test
    void testToscaTopologyTemplate() {
        var topologyTemplate =  new ToscaTopologyTemplate();
        var doc = new DocToscaTopologyTemplate(topologyTemplate);
        assertThat(doc)
                .isNotEqualByComparingTo(null)
                .isEqualByComparingTo(doc);
        var doc2 = new DocToscaTopologyTemplate(doc);
        assertThat(doc).isEqualByComparingTo(doc2);

        var nodeTemplate = new ToscaNodeTemplate();
        nodeTemplate.setType("type");
        nodeTemplate.setTypeVersion("1.0.0");
        topologyTemplate.setNodeTemplates(Map.of(NAME, nodeTemplate));
        doc = new DocToscaTopologyTemplate(topologyTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setNodeTemplates(doc.getNodeTemplates());
        assertThat(doc).isEqualByComparingTo(doc2);

        var toscaPolicy = new ToscaPolicy();
        toscaPolicy.setType("type");
        toscaPolicy.setTypeVersion("1.0.0");
        topologyTemplate.setPolicies(List.of(Map.of(NAME, toscaPolicy)));
        doc = new DocToscaTopologyTemplate(topologyTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setPolicies(doc.getPolicies());
        assertThat(doc).isEqualByComparingTo(doc2);

        topologyTemplate.setInputs(Map.of(NAME, new ToscaParameter()));
        doc = new DocToscaTopologyTemplate(topologyTemplate);
        assertThat(doc).isNotEqualByComparingTo(doc2);
        doc2.setInputs(doc.getInputs());
        assertThat(doc).isEqualByComparingTo(doc2);
        var docParameter = doc.getInputs().get(NAME);
        assertThat(docParameter)
                .isNotEqualByComparingTo(null)
                .isEqualByComparingTo(docParameter);

        doc2 = new DocToscaTopologyTemplate(doc);
        assertThat(doc.toAuthorative()).isEqualTo(doc2.toAuthorative());
    }
}

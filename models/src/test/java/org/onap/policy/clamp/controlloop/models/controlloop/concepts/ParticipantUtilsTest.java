/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.controlloop.concepts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class ParticipantUtilsTest {

    private static final Coder CODER = new StandardCoder();
    private static final String TOSCA_TEMPLATE_YAML = "examples/controlloop/PMSubscriptionHandling.yaml";
    private static final String CONTROL_LOOP_JSON = "src/test/resources/providers/TestControlLoops.json";
    private static final String CONTROL_LOOP_ELEMENT = "org.onap.policy.clamp.controlloop.ControlLoopElement";
    private static final String POLICY_CONTROL_LOOP_ELEMENT =
            "org.onap.policy.clamp.controlloop.PolicyControlLoopElement";
    private static final String PARTICIPANT_CONTROL_LOOP_ELEMENT = "org.onap.policy.clamp.controlloop.Participant";
    private static final StandardYamlCoder YAML_TRANSLATOR = new StandardYamlCoder();

    @Test
    void testFindParticipantType() throws CoderException {
        var identifier = new ToscaConceptIdentifier("Identifier", "1.0.1");
        var result = ParticipantUtils.findParticipantType(Map.of("participantType", CODER.encode(identifier)));
        assertThat(result).isEqualTo(identifier);
    }

    @Test
    void testFindStartPhase() {
        var identifier = 13;
        var result = ParticipantUtils.findStartPhase(Map.of("startPhase", identifier));
        assertThat(result).isEqualTo(identifier);
    }

    @Test
    void testGetFirstStartPhase() throws CoderException {
        var serviceTemplate = YAML_TRANSLATOR.decode(ResourceUtils.getResourceAsStream(TOSCA_TEMPLATE_YAML),
                ToscaServiceTemplate.class);
        var controlLoops = CODER.decode(ResourceUtils.getResourceAsString(CONTROL_LOOP_JSON), ControlLoops.class);
        var result = ParticipantUtils.getFirstStartPhase(controlLoops.getControlLoopList().get(0), serviceTemplate);
        assertThat(result).isZero();
    }

    @Test
    void testCheckIfNodeTemplateIsControlLoopElement() throws CoderException {
        var serviceTemplate = YAML_TRANSLATOR.decode(ResourceUtils.getResourceAsStream(TOSCA_TEMPLATE_YAML),
                ToscaServiceTemplate.class);
        var nodeTemplate = new ToscaNodeTemplate();
        nodeTemplate.setType(CONTROL_LOOP_ELEMENT);
        assertThat(ParticipantUtils.checkIfNodeTemplateIsControlLoopElement(nodeTemplate, serviceTemplate)).isTrue();

        nodeTemplate.setType(POLICY_CONTROL_LOOP_ELEMENT);
        assertThat(ParticipantUtils.checkIfNodeTemplateIsControlLoopElement(nodeTemplate, serviceTemplate)).isTrue();

        nodeTemplate.setType(PARTICIPANT_CONTROL_LOOP_ELEMENT);
        assertThat(ParticipantUtils.checkIfNodeTemplateIsControlLoopElement(nodeTemplate, serviceTemplate)).isFalse();
    }
}

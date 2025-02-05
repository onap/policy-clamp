/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023,2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.concepts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;

class ParticipantUtilsTest {

    private static final Coder CODER = new StandardCoder();
    private static final String TOSCA_TEMPLATE_YAML = "examples/acm/test-pm-subscription-handling.yaml";
    private static final String AUTOMATION_COMPOSITION_JSON =
        "src/test/resources/providers/TestAutomationCompositions.json";

    private static final String PROPERTIES = """
            stage:
              prepare: [1,2]
              migrate: [2,3]
            """;


    @Test
    void testFindStartPhase() {
        var identifier = 13;
        var result = ParticipantUtils.findStartPhase(Map.of("startPhase", identifier));
        assertThat(result).isEqualTo(identifier);
    }

    @Test
    void testGetFirstStartPhase() throws CoderException {
        var serviceTemplate = CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);
        var automationComposition =
            CODER.decode(ResourceUtils.getResourceAsString(AUTOMATION_COMPOSITION_JSON), AutomationCompositions.class)
                    .getAutomationCompositionList().get(0);
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        var result = ParticipantUtils.getFirstStartPhase(automationComposition, serviceTemplate);
        assertThat(result).isZero();

        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.UNLOCKING);
        result = ParticipantUtils.getFirstStartPhase(automationComposition, serviceTemplate);
        assertThat(result).isZero();

        automationComposition.setDeployState(DeployState.UNDEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        result = ParticipantUtils.getFirstStartPhase(automationComposition, serviceTemplate);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testGetFirstStartPhaseWithNull() throws CoderException {
        var serviceTemplate = CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);
        var automationComposition =
            CODER.decode(ResourceUtils.getResourceAsString(AUTOMATION_COMPOSITION_JSON), AutomationCompositions.class)
                    .getAutomationCompositionList().get(0);
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setLockState(LockState.NONE);

        serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().values()
            .forEach(node -> node.setVersion("0.0.0"));
        var result = ParticipantUtils.getFirstStartPhase(automationComposition, serviceTemplate);
        assertThat(result).isZero();

        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.UNLOCKING);
        result = ParticipantUtils.getFirstStartPhase(automationComposition, serviceTemplate);
        assertThat(result).isZero();

        automationComposition.setDeployState(DeployState.UNDEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        result = ParticipantUtils.getFirstStartPhase(automationComposition, serviceTemplate);
        assertThat(result).isZero();

        serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().clear();
        result = ParticipantUtils.getFirstStartPhase(automationComposition, serviceTemplate);
        assertThat(result).isZero();

        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.UNLOCKING);
        result = ParticipantUtils.getFirstStartPhase(automationComposition, serviceTemplate);
        assertThat(result).isZero();

        automationComposition.setDeployState(DeployState.UNDEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        result = ParticipantUtils.getFirstStartPhase(automationComposition, serviceTemplate);
        assertThat(result).isZero();
    }

    @Test
    void testGetFirstStage() throws CoderException {
        var serviceTemplate = CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);
        var automationComposition =
            CODER.decode(ResourceUtils.getResourceAsString(AUTOMATION_COMPOSITION_JSON), AutomationCompositions.class)
                    .getAutomationCompositionList().get(0);
        automationComposition.setDeployState(DeployState.MIGRATING);
        var result = ParticipantUtils.getFirstStage(automationComposition, serviceTemplate);
        assertThat(result).isZero();

        automationComposition.setDeployState(DeployState.UNDEPLOYED);
        automationComposition.setSubState(SubState.PREPARING);
        result = ParticipantUtils.getFirstStage(automationComposition, serviceTemplate);
        assertThat(result).isZero();
    }

    @Test
    void testFindStageSetPrepare() {
        var result = ParticipantUtils.findStageSetPrepare(Map.of());
        assertThat(result).hasSize(1).contains(0);
        result = ParticipantUtils.findStageSetPrepare(Map.of("stage", 1));
        assertThat(result).hasSize(1).contains(0);

        Map<String, Object> map = CommonTestData.getObject(PROPERTIES, Map.class);
        result = ParticipantUtils.findStageSetPrepare(map);
        assertThat(result).hasSize(2).contains(1).contains(2);
    }

    @Test
    void testFindStageSetMigrate() {
        var result = ParticipantUtils.findStageSetMigrate(Map.of());
        assertThat(result).hasSize(1).contains(0);
        result = ParticipantUtils.findStageSetMigrate(Map.of("stage", 1));
        assertThat(result).hasSize(1).contains(0);

        Map<String, Object> map = CommonTestData.getObject(PROPERTIES, Map.class);
        result = ParticipantUtils.findStageSetMigrate(map);
        assertThat(result).hasSize(2).contains(2).contains(3);
    }
}

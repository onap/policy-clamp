/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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
package org.onap.policy.clamp.acm.runtime.helper

import org.onap.policy.clamp.acm.runtime.instantiation.AutomationCompositionInstantiationProvider
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.AcInstanceStateResolver
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ProviderUtils
import org.onap.policy.models.base.PfModelRuntimeException
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate
import org.yaml.snakeyaml.Yaml

class InstantiationProviderTestHelper {

    private static final String CONFIG_PATH = "instantiation/instantiation-provider-test-config.yaml"

    private final Map config
    private final ToscaServiceTemplate serviceTemplate
    private final ToscaServiceTemplate serviceTemplateMigration

    AutomationCompositionProvider acProvider
    AcDefinitionProvider acDefinitionProvider
    SupervisionAcHandler supervisionAcHandler
    ParticipantProvider participantProvider

    InstantiationProviderTestHelper() {
        this.config = new Yaml().load(getClass().classLoader.getResourceAsStream(CONFIG_PATH))
        this.serviceTemplate = toAuthorative(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
        this.serviceTemplateMigration = toAuthorative(config.resourcePaths.migrationYaml)
    }

    String getResourcePath(String key) {
        config.resourcePaths[key]
    }

    String getErrorMessage(String key) {
        config.errorMessages[key]
    }

    private static ToscaServiceTemplate toAuthorative(String path) {
        def st = InstantiationUtils.getToscaServiceTemplate(path)
        ProviderUtils.getJpaAndValidate(st, DocToscaServiceTemplate::new, "toscaServiceTemplate").toAuthorative()
    }

    AutomationCompositionInstantiationProvider createProvider(
            AcRuntimeParameterGroup params = CommonTestData.getTestParamaterGroup(),
            EncryptionUtils encryption = new EncryptionUtils(CommonTestData.getTestParamaterGroup())) {
        new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider, params, encryption)
    }

    def createPrimedDefinition(ToscaServiceTemplate template = serviceTemplate) {
        CommonTestData.createAcDefinition(template, AcTypeState.PRIMED)
    }

    AutomationComposition loadAc(String resourceKey, String suffix) {
        InstantiationUtils.getAutomationCompositionFromResource(getResourcePath(resourceKey), suffix)
    }

    AutomationComposition loadCustomAc(String resourceKey, String suffix, deployState, UUID compositionId) {
        InstantiationUtils.getCustomAutomationComposition(getResourcePath(resourceKey), suffix, deployState, compositionId)
    }

    def createDeleteProvider(AutomationComposition ac, deployState, lockState,
                             AutomationCompositionProvider localAcProvider,
                             AcDefinitionProvider localAcDefProvider,
                             ParticipantProvider localParticipantProvider,
                             AcRuntimeParameterGroup localParams) {
        ac.deployState = deployState
        ac.lockState = lockState

        def acDef = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED)
        ac.compositionId = acDef.compositionId

        def provider = new AutomationCompositionInstantiationProvider(localAcProvider, localAcDefProvider,
                new AcInstanceStateResolver(), null, localParticipantProvider, localParams, null)

        [acDef: acDef, provider: provider]
    }
}

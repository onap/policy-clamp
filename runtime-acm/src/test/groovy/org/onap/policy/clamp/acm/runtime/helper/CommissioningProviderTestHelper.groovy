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

import org.onap.policy.clamp.acm.runtime.commissioning.CommissioningProvider
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition
import org.onap.policy.clamp.models.acm.persistence.provider.AcTypeStateResolver
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantPrimePublisher
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate
import org.yaml.snakeyaml.Yaml

class CommissioningProviderTestHelper {

    private static final String CONFIG_PATH = "commissioning/commissioning-provider-test-config.yaml"

    private final Map config

    AcDefinitionProvider acDefinitionProvider
    AutomationCompositionProvider acProvider
    ParticipantProvider participantProvider
    ParticipantPrimePublisher participantPrimePublisher

    CommissioningProviderTestHelper() {
        this.config = new Yaml().load(getClass().classLoader.getResourceAsStream(CONFIG_PATH))
    }

    int getExpectedAffectedDefinitionsCount() {
        config.expectedAffectedDefinitionsCount as int
    }

    int getLatchTimeoutSeconds() {
        config.latchTimeoutSeconds as int
    }

    String getErrorMessage(String key) {
        config.errorMessages[key]
    }

    static ToscaServiceTemplate loadServiceTemplate() {
        InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
    }

    CommissioningProvider createProvider(AcRuntimeParameterGroup params = null) {
        new CommissioningProvider(acDefinitionProvider, acProvider, participantProvider,
                new AcTypeStateResolver(), participantPrimePublisher, params)
    }

    static AutomationCompositionDefinition createAcmDefinition(String name = null, String version = null) {
        def serviceTemplate = loadServiceTemplate()
        if (name) serviceTemplate.name = name
        if (version) serviceTemplate.version = version
        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: UUID.randomUUID(), serviceTemplate: serviceTemplate)
        acmDefinition
    }
}

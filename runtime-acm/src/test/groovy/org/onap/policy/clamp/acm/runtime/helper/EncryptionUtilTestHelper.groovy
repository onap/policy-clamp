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

import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate
import org.onap.policy.clamp.models.acm.persistence.provider.ProviderUtils
import org.yaml.snakeyaml.Yaml

class EncryptionUtilTestHelper {

    private static final String CONFIG_PATH = "encryption/encryption-util-test-config.yaml"

    private final Map config
    private final AutomationCompositionDefinition acDefinition

    EncryptionUtilTestHelper() {
        this.config = new Yaml().load(getClass().classLoader.getResourceAsStream(CONFIG_PATH))
        def st = InstantiationUtils.getToscaServiceTemplate(config.resourcePaths.toscaTemplate)
        def jpa = ProviderUtils.getJpaAndValidate(st, DocToscaServiceTemplate::new, "toscaServiceTemplate")
        this.acDefinition = CommonTestData.createAcDefinition(jpa.toAuthorative(), AcTypeState.PRIMED)
    }

    String getResourcePath(String key) {
        config.resourcePaths[key]
    }

    String getExpectedValue(String key) {
        config.expectedValues[key]
    }

    String getErrorMessage(String key) {
        config.errorMessages[key]
    }

    AutomationComposition loadAc() {
        InstantiationUtils.getAutomationCompositionFromResource(
                getResourcePath("instantiateJson"), config.acSuffix as String)
    }

    static EncryptionUtils createEncryption() {
        new EncryptionUtils(CommonTestData.getEncryptionParameterGroup())
    }
}

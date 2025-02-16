/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.main.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.persistence.provider.ProviderUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;

class EncryptionUtilTest {

    private static ToscaServiceTemplate serviceTemplateEncrypt;
    public static final String TOSCA_TEMPLATE_YAML = "src/test/resources/providers/AcDefinitionEncryptTest.yaml";
    public static final String INSTANTIATE_JSON = "src/test/resources/providers/AcInstantiateEncryptTest.json";
    private static AutomationCompositionDefinition acDefinition;

    @BeforeAll
    public static void setUpBeforeClass() {
        serviceTemplateEncrypt = InstantiationUtils.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);
        var jpa2 = ProviderUtils.getJpaAndValidate(serviceTemplateEncrypt, JpaToscaServiceTemplate::new,
                "toscaServiceTemplate");
        serviceTemplateEncrypt = jpa2.toAuthorative();
        acDefinition = CommonTestData.createAcDefinition(serviceTemplateEncrypt, AcTypeState.PRIMED);
    }

    @Test
    void testEncryptAcInstanceProperties() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(INSTANTIATE_JSON, "Crud");
        var encryptionUtils = new EncryptionUtils(CommonTestData.getEncryptionParamaterGroup());
        assertTrue(encryptionUtils.encryptionEnabled());
        assertDoesNotThrow(()
                -> {
            assert automationComposition != null;
            encryptionUtils.findAndEncryptSensitiveData(acDefinition, automationComposition);
        });
        assertDoesNotThrow(() -> {
            assert automationComposition != null;
            encryptionUtils.findAndDecryptSensitiveData(automationComposition);
        });
    }

}

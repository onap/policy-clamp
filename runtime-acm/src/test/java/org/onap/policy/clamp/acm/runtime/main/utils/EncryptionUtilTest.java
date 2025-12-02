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

package org.onap.policy.clamp.acm.runtime.main.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.InvalidAlgorithmParameterException;
import java.util.List;
import javax.crypto.Cipher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate;
import org.onap.policy.clamp.models.acm.persistence.provider.ProviderUtils;

class EncryptionUtilTest {

    private static final String TOSCA_TEMPLATE_YAML = "src/test/resources/providers/AcDefinitionEncryptTest.yaml";
    private static final String INSTANTIATE_JSON = "src/test/resources/providers/AcInstantiateEncryptTest.json";
    private static AutomationCompositionDefinition acDefinition;

    @BeforeAll
    static void setUpBeforeClass() {
        var serviceTemplateEncrypt = InstantiationUtils.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);
        var jpa2 = ProviderUtils.getJpaAndValidate(serviceTemplateEncrypt, DocToscaServiceTemplate::new,
                "toscaServiceTemplate");
        serviceTemplateEncrypt = jpa2.toAuthorative();
        acDefinition = CommonTestData.createAcDefinition(serviceTemplateEncrypt, AcTypeState.PRIMED);
    }

    @Test
    void testEncryptAcInstanceProperties() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(INSTANTIATE_JSON, "Crud");
        var encryptionUtils = new EncryptionUtils(CommonTestData.getEncryptionParameterGroup());
        assertTrue(encryptionUtils.encryptionEnabled());
        assert automationComposition != null;
        assertDoesNotThrow(() -> encryptionUtils.findAndEncryptSensitiveData(acDefinition, automationComposition));

        automationComposition.getElements().values().forEach(element -> {
            assertTrue(element.getProperties().get("secret").toString().startsWith("ENCRYPTED:"));
            assertTrue(element.getProperties().get("password").toString().startsWith("ENCRYPTED:"));
        });

        var encryptionUtil2 = new EncryptionUtils(CommonTestData.getEncryptionParameterGroup());
        assertDoesNotThrow(() -> encryptionUtil2.decryptInstanceProperties(automationComposition));
        assertDoesNotThrow(() -> encryptionUtil2.decryptInstanceProperties(List.of(automationComposition)));
        automationComposition.getElements().values().forEach(element -> {
            assertEquals("mysecret", element.getProperties().get("secret").toString());
            assertEquals("mypass", element.getProperties().get("password").toString());
        });
    }

    @Test
    void testErrorScenario() {
        var encryptionUtils = new EncryptionUtils(CommonTestData.getEncryptionParameterGroup()) {
            @Override
            protected Cipher getCipher(byte[] iv, int mode) throws InvalidAlgorithmParameterException {
                throw new InvalidAlgorithmParameterException();
            }
        };
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(INSTANTIATE_JSON, "Crud");
        assertDoesNotThrow(() -> encryptionUtils.findAndEncryptSensitiveData(acDefinition, null));

        var encryptionUtils2 = new EncryptionUtils(CommonTestData.getEncryptionParameterGroup());
        encryptionUtils2.findAndEncryptSensitiveData(acDefinition, automationComposition);

        assert automationComposition != null;
        assertThrows(AutomationCompositionRuntimeException.class,
                () -> encryptionUtils.decryptInstanceProperties(automationComposition));
    }

}

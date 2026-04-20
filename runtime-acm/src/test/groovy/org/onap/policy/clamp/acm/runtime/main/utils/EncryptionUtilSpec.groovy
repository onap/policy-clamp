/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
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
package org.onap.policy.clamp.acm.runtime.main.utils

import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate
import org.onap.policy.clamp.models.acm.persistence.provider.ProviderUtils
import spock.lang.Shared
import spock.lang.Specification

import javax.crypto.Cipher
import java.security.InvalidAlgorithmParameterException

class EncryptionUtilSpec extends Specification {

    static final TOSCA_TEMPLATE = "src/test/resources/providers/AcDefinitionEncryptTest.yaml"
    static final INSTANTIATE_JSON = "src/test/resources/providers/AcInstantiateEncryptTest.json"

    @Shared AutomationCompositionDefinition acDefinition

    def setupSpec() {
        def st = InstantiationUtils.getToscaServiceTemplate(TOSCA_TEMPLATE)
        def jpa = ProviderUtils.getJpaAndValidate(st, DocToscaServiceTemplate::new, "toscaServiceTemplate")
        acDefinition = CommonTestData.createAcDefinition(jpa.toAuthorative(), AcTypeState.PRIMED)
    }

    private static loadAc() {
        InstantiationUtils.getAutomationCompositionFromResource(INSTANTIATE_JSON, "Crud")
    }

    def "encrypt and decrypt restores original values"() {
        given:
        def ac = loadAc()
        def encryption = new EncryptionUtils(CommonTestData.getEncryptionParameterGroup())

        expect:
        encryption.encryptionEnabled()

        when:
        encryption.findAndEncryptSensitiveData(acDefinition, ac)

        then:
        ac.elements.values().every {
            it.properties.secret.startsWith("ENCRYPTED:") && it.properties.password.startsWith("ENCRYPTED:")
        }

        when:
        encryption.decryptInstanceProperties(ac)

        then:
        ac.elements.values().every {
            it.properties.secret == "mysecret" && it.properties.password == "mypass"
        }
    }

    def "null AC input does not throw"() {
        given:
        def encryption = new EncryptionUtils(CommonTestData.getEncryptionParameterGroup())

        when:
        encryption.findAndEncryptSensitiveData(acDefinition, null)

        then:
        noExceptionThrown()
    }

    def "faulty cipher causes decrypt to throw"() {
        given:
        def faultyEncryption = new EncryptionUtils(CommonTestData.getEncryptionParameterGroup()) {
            @Override
            protected Cipher getCipher(byte[] iv, int mode) {
                throw new InvalidAlgorithmParameterException("Simulated failure")
            }
        }
        def ac = loadAc()
        new EncryptionUtils(CommonTestData.getEncryptionParameterGroup())
                .findAndEncryptSensitiveData(acDefinition, ac)

        when:
        faultyEncryption.decryptInstanceProperties(ac)

        then:
        def ex = thrown(AutomationCompositionRuntimeException)
        ex.message.contains("decrypt")
    }
}

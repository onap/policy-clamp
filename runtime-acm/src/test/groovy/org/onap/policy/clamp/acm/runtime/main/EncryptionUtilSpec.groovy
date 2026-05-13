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
package org.onap.policy.clamp.acm.runtime.main

import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition
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
    static final AC_SUFFIX = "Crud"

    @Shared
    AutomationCompositionDefinition acDefinition

    def setupSpec() {
        def st = InstantiationUtils.getToscaServiceTemplate(TOSCA_TEMPLATE)
        def jpa = ProviderUtils.getJpaAndValidate(st, DocToscaServiceTemplate::new, "toscaServiceTemplate")
        acDefinition = CommonTestData.createAcDefinition(jpa.toAuthorative(), AcTypeState.PRIMED)
    }

    def "encrypt and decrypt should restore original #property value"() {
        given: "an automation composition with plaintext properties"
        def ac = loadAc()
        def encryption = createEncryption()

        expect: "encryption should be enabled"
        encryption.encryptionEnabled()

        when: "sensitive data is encrypted and then decrypted"
        encryption.findAndEncryptSensitiveData(acDefinition, ac)
        def encryptedVal = ac.elements.values().first().properties[property]
        encryption.decryptInstanceProperties(ac)
        def decryptedVal = ac.elements.values().first().properties[property]

        then: "encrypted value starts with prefix and decrypted value matches original"
        encryptedVal.startsWith("ENCRYPTED:")
        decryptedVal == expectedPlaintext

        where:
        property   | expectedPlaintext
        "secret"   | "mysecret"
        "password" | "mypass"
    }

    def "given a null AC input, findAndEncryptSensitiveData should not throw any exception"() {
        given: "an encryption utility instance"
        def encryption = createEncryption()

        when: "findAndEncryptSensitiveData is called with null AC"
        encryption.findAndEncryptSensitiveData(acDefinition, null)

        then: "no exception should be thrown"
        noExceptionThrown()
    }

    def "given a faulty cipher, decryptInstanceProperties should throw AutomationCompositionRuntimeException"() {
        given: "an encryption utility with a cipher that always throws InvalidAlgorithmParameterException"
        def faultyEncryption = new EncryptionUtils(CommonTestData.getEncryptionParameterGroup()) {
            @Override
            protected Cipher getCipher(byte[] iv, int mode) {
                throw new InvalidAlgorithmParameterException("Simulated failure")
            }
        }
        def ac = loadAc()
        createEncryption().findAndEncryptSensitiveData(acDefinition, ac)

        when: "decryption is attempted with the faulty cipher"
        faultyEncryption.decryptInstanceProperties(ac)

        then: "an AutomationCompositionRuntimeException should be thrown with a decrypt failure message"
        def ex = thrown(AutomationCompositionRuntimeException)
        ex.message.contains("Failed to decrypt instance field")
    }

    def loadAc() {
        return InstantiationUtils.getAutomationCompositionFromResource(INSTANTIATE_JSON, AC_SUFFIX)
    }

    def createEncryption() {
        return new EncryptionUtils(CommonTestData.getEncryptionParameterGroup())
    }
}

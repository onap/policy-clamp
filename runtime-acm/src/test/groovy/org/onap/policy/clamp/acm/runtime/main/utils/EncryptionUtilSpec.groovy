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
package org.onap.policy.clamp.acm.runtime.main.utils

import org.onap.policy.clamp.acm.runtime.helper.EncryptionUtilTestHelper
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException
import spock.lang.Shared
import spock.lang.Specification

import javax.crypto.Cipher
import java.security.InvalidAlgorithmParameterException

class EncryptionUtilSpec extends Specification {

    @Shared
    EncryptionUtilTestHelper helper = new EncryptionUtilTestHelper()

    def "given an AC with valid properties, encrypt and decrypt should restore original secret and password values"() {
        given: "an automation composition with plaintext secret and password properties"
        def ac = helper.loadAc()
        def encryption = helper.createEncryption()

        expect: "encryption should be enabled"
        encryption.encryptionEnabled()

        when: "sensitive data is encrypted"
        encryption.findAndEncryptSensitiveData(helper.acDefinition, ac)

        then: "all element properties should start with the encrypted prefix"
        ac.elements.values().every {
            it.properties.secret.startsWith(helper.getExpectedValue("encryptedPrefix")) &&
                    it.properties.password.startsWith(helper.getExpectedValue("encryptedPrefix"))
        }

        when: "the encrypted properties are decrypted"
        encryption.decryptInstanceProperties(ac)

        then: "the original plaintext values should be restored"
        ac.elements.values().every {
            it.properties.secret == helper.getExpectedValue("secret") &&
                    it.properties.password == helper.getExpectedValue("password")
        }
    }

    def "given a null AC input, findAndEncryptSensitiveData should not throw any exception"() {
        given: "an encryption utility instance"
        def encryption = helper.createEncryption()

        when: "findAndEncryptSensitiveData is called with null AC"
        encryption.findAndEncryptSensitiveData(helper.acDefinition, null)

        then: "no exception should be thrown"
        noExceptionThrown()
    }

    def "given a faulty cipher, decryptInstanceProperties should throw AutomationCompositionRuntimeException"() {
        given: "an encryption utility with a cipher that always throws InvalidAlgorithmParameterException"
        def faultyEncryption = new EncryptionUtils(org.onap.policy.clamp.acm.runtime.util.CommonTestData.getEncryptionParameterGroup()) {
            @Override
            protected Cipher getCipher(byte[] iv, int mode) {
                throw new InvalidAlgorithmParameterException("Simulated failure")
            }
        }
        def ac = helper.loadAc()
        helper.createEncryption().findAndEncryptSensitiveData(helper.acDefinition, ac)

        when: "decryption is attempted with the faulty cipher"
        faultyEncryption.decryptInstanceProperties(ac)

        then: "an AutomationCompositionRuntimeException should be thrown with a decrypt failure message"
        def ex = thrown(AutomationCompositionRuntimeException)
        ex.message.contains(helper.getErrorMessage("decryptFailure"))
    }
}

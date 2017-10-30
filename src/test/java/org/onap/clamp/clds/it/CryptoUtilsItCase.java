/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.security.GeneralSecurityException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.util.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test Crypto Utils with Spring.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:application-no-camunda.properties")
public class CryptoUtilsItCase {
    @Autowired
    private CryptoUtils cryptoUtils;

    /**
     * This method tests encryption.
     * 
     * @throws GeneralSecurityException
     */
    @Test
    public final void testEncryption() throws GeneralSecurityException {
        final String testData = "This is a test string";
        final String encodedStringExpected = "A5CB112C9F608A220B35AFED08024D98B9653333AF4C9527C2E934DE473F6145";
        String encodedString = cryptoUtils.encrypt(testData);
        assertNotNull(encodedString);
        assertEquals(encodedStringExpected, encodedString);
    }

    /**
     * This method tests decryption.
     * 
     * @throws GeneralSecurityException
     */
    @Test
    public final void testDecryption() throws GeneralSecurityException {
        final String decodedStringExpected = "This is a test string";
        final String encodedString = "A5CB112C9F608A220B35AFED08024D98B9653333AF4C9527C2E934DE473F6145";
        String decryptedString = cryptoUtils.decrypt(encodedString);
        assertNotNull(decryptedString);
        assertEquals(decodedStringExpected, decryptedString);
    }
}
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

package org.onap.clamp.clds.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;


public class CryptoUtilsTest {

    private final String data = "This is a test string";

    @Test
    public final void testEncryption() throws Exception {
        String encodedString = CryptoUtils.encrypt(data);
        assertNotNull(encodedString);
        assertEquals(data, CryptoUtils.decrypt(encodedString));
    }

    @Test
    public final void testEncryptedStringIsDifferent() throws Exception {
        String encodedString1 = CryptoUtils.encrypt(data);
        String encodedString2 = CryptoUtils.encrypt(data);
        byte[] encryptedMessage1 = Hex.decodeHex(encodedString1.toCharArray());
        byte[] encryptedMessage2 = Hex.decodeHex(encodedString2.toCharArray());
        assertNotNull(encryptedMessage1);
        assertNotNull(encryptedMessage2);
        assertNotEquals(encryptedMessage1, encryptedMessage2);
        byte[] subData1 = ArrayUtils.subarray(encryptedMessage1, 16, encryptedMessage1.length);
        byte[] subData2 = ArrayUtils.subarray(encryptedMessage2, 16, encryptedMessage2.length);
        assertNotEquals(subData1, subData2);
    }
}
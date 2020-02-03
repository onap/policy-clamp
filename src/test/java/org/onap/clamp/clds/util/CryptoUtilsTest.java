/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
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
 * 
 */

package org.onap.clamp.clds.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;

import java.security.InvalidKeyException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.crypto.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class CryptoUtilsTest {

    private final String data = "This is a test string";

    @Test
    @PrepareForTest({ CryptoUtils.class })
    public final void testEncryption() throws Exception {
        String encodedString = CryptoUtils.encrypt(data);
        assertNotNull(encodedString);
        assertEquals(data, CryptoUtils.decrypt(encodedString));
    }

    @Test
    @PrepareForTest({ CryptoUtils.class })
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

    @Test
    @PrepareForTest({ CryptoUtils.class })
    public final void testEncryptionBaseOnRandomKey() throws Exception {
        SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();
        final String encryptionKey = String.valueOf(Hex.encodeHex(secretKey.getEncoded()));
        setAesEncryptionKeyEnv(encryptionKey);

        String encodedString = CryptoUtils.encrypt(data);
        String decodedString = CryptoUtils.decrypt(encodedString);
        assertEquals(data, decodedString);
    }

    @Test(expected = InvalidKeyException.class)
    @PrepareForTest({ CryptoUtils.class })
    public final void testEncryptionBadKey() throws Exception {
        final String badEncryptionKey = "93210sd";
        setAesEncryptionKeyEnv(badEncryptionKey);

        CryptoUtils.encrypt(data);
    }

    private static void setAesEncryptionKeyEnv(String value) {
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv(eq("AES_ENCRYPTION_KEY"))).thenReturn(value);
    }
}

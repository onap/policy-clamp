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

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * CryptoUtils for encrypting/decrypting string based on a Key defined in
 * application.properties (Spring config file).
 * 
 */
@Component("CryptoUtils")
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class CryptoUtils {
    public static final String AES           = "AES";
    public static final String KEY_PARAM     = "org.onap.clamp.encryption.aes.key";
    private SecretKeySpec      secretKeySpec = getSecretKeySpec("aa3871669d893c7fb8abbcda31b88b4f");

    /**
     * Encrypt a value based on the Clamp Encryption Key.
     * 
     * @param value
     * @return The encrypted string
     * @throws GeneralSecurityException
     *             In case of issue with the encryption
     */
    public String encrypt(String value) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CryptoUtils.AES);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, cipher.getParameters());
        byte[] encrypted = cipher.doFinal(value.getBytes());
        return byteArrayToHexString(encrypted);
    }

    /**
     * Decrypt a value.
     * 
     * @param message
     *            The encrypted string that must be decrypted using the Clamp
     *            Encryption Key
     * @return The String decrypted
     * @throws GeneralSecurityException
     *             In case of issue with the encryption
     */
    public String decrypt(String message) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CryptoUtils.AES);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] decrypted = cipher.doFinal(hexStringToByteArray(message));
        return new String(decrypted);
    }

    private SecretKeySpec getSecretKeySpec(String keyString) {
        byte[] key = hexStringToByteArray(keyString);
        return new SecretKeySpec(key, CryptoUtils.AES);
    }

    private String byteArrayToHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase();
    }

    private byte[] hexStringToByteArray(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }
}

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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

/**
 * CryptoUtils for encrypting/decrypting string based on a Key defined in
 * application.properties (Spring config file).
 * 
 */
public final class CryptoUtils {
    protected static final EELFLogger logger            = EELFManager.getInstance().getLogger(CryptoUtils.class);
    // Openssl commands:
    // Encrypt: echo -n "123456" | openssl aes-128-cbc -e -K <Private Hex key>
    // -iv <16 Hex Bytes iv> | xxd -u -g100
    // Final result is to put in properties file is: IV + Outcome of openssl
    // command
    // ************************************************************
    // Decrypt: echo -n 'Encrypted string' | xxd -r -ps | openssl aes-128-cbc -d
    // -K
    // <Private Hex Key> -iv <16 Bytes IV extracted from Encrypted String>
    private static final String       ALGORITHM         = "AES";
    private static final String       ALGORYTHM_DETAILS = ALGORITHM + "/CBC/PKCS5PADDING";
    private static final int          BLOCK_SIZE        = 128;
    private static final String       KEY_PARAM         = "org.onap.clamp.encryption.aes.key";
    private static SecretKeySpec      secretKeySpec     = null;
    private IvParameterSpec           ivspec;
    static {
        Properties props = new Properties();
        try {
            props.load(ResourceFileUtil.getResourceAsStream("clds/key.properties"));
            secretKeySpec = getSecretKeySpec(props.getProperty(KEY_PARAM));
        } catch (IOException | DecoderException e) {
            logger.error("Exception occurred during the key reading", e);
        }
    }

    /**
     * Encrypt a value based on the Clamp Encryption Key.
     * 
     * @param value
     * @return The encrypted string
     * @throws GeneralSecurityException
     *             In case of issue with the encryption
     * @throws UnsupportedEncodingException
     *             In case of issue with the charset conversion
     */
    public String encrypt(String value) throws GeneralSecurityException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance(CryptoUtils.ALGORYTHM_DETAILS, "SunJCE");
        SecureRandom r = SecureRandom.getInstance("SHA1PRNG");
        byte[] iv = new byte[BLOCK_SIZE / 8];
        r.nextBytes(iv);
        ivspec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivspec);
        return Hex.encodeHexString(ArrayUtils.addAll(iv, cipher.doFinal(value.getBytes("UTF-8"))));
    }

    /**
     * Decrypt a value based on the Clamp Encryption Key
     * 
     * @param message
     *            The encrypted string that must be decrypted using the Clamp
     *            Encryption Key
     * @return The String decrypted
     * @throws GeneralSecurityException
     *             In case of issue with the encryption
     * @throws DecoderException
     *             In case of issue to decode the HexString
     */
    public String decrypt(String message) throws GeneralSecurityException, DecoderException {
        byte[] encryptedMessage = Hex.decodeHex(message.toCharArray());
        Cipher cipher = Cipher.getInstance(CryptoUtils.ALGORYTHM_DETAILS, "SunJCE");
        ivspec = new IvParameterSpec(ArrayUtils.subarray(encryptedMessage, 0, BLOCK_SIZE / 8));
        byte[] realData = ArrayUtils.subarray(encryptedMessage, BLOCK_SIZE / 8, encryptedMessage.length);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivspec);
        byte[] decrypted = cipher.doFinal(realData);
        return new String(decrypted);
    }

    private static SecretKeySpec getSecretKeySpec(String keyString) throws DecoderException {
        byte[] key = Hex.decodeHex(keyString.toCharArray());
        return new SecretKeySpec(key, CryptoUtils.ALGORITHM);
    }
}

/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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
 * 
 */

package org.onap.clamp.clds.util;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.common.base.Charsets;

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
 */
public final class CryptoUtils {

    /**
     * Used to log CryptoUtils class.
     */
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(CryptoUtils.class);
    // Openssl commands:
    // Encrypt: echo -n "123456" | openssl aes-128-cbc -e -K <Private Hex key>
    // -iv <16 Bytes iv (HEX), be careful it's 32 Hex Chars> | xxd -u -g100
    // Final result is to put in properties file is: IV + Outcome of openssl
    // command
    // ************************************************************
    // Decrypt: echo -n 'Encrypted string' | xxd -r -ps | openssl aes-128-cbc -d
    // -K
    // <Private Hex Key> -iv <16 Bytes IV extracted from Encrypted String, be
    // careful it's 32 Hex Chars>
    /**
     * Definition of encryption algorithm.
     */
    private static final String ALGORITHM = "AES";
    
    /**
     * AES Encryption Key environment variable for external configuration
     */
    private static final String AES_ENCRYPTION_KEY = "AES_ENCRYPTION_KEY";
    
    /**
     * Detailed definition of encryption algorithm.
     */
    private static final String ALGORITHM_DETAILS = ALGORITHM + "/CBC/PKCS5PADDING";
    private static final int IV_BLOCK_SIZE_IN_BITS = 128;
    /**
     * An Initial Vector of 16 Bytes, so 32 Hexadecimal Chars.
     */
    private static final int IV_BLOCK_SIZE_IN_BYTES = IV_BLOCK_SIZE_IN_BITS / 8;
    /**
     * Key to read in the key.properties file.
     */
    private static final String KEY_PARAM = "org.onap.clamp.encryption.aes.key";
    private static final String PROPERTIES_FILE_NAME = "clds/key.properties";
    /**
     * The SecretKeySpec created from the Base 64 String key.
     */
    private static final SecretKeySpec SECRET_KEY_SPEC = readSecretKeySpec(PROPERTIES_FILE_NAME);

    /**
     * Private constructor to avoid creating instances of util class.
     */
    private CryptoUtils() {
    }

    /**
     * Encrypt a value based on the Clamp Encryption Key.
     * 
     * @param value
     *            The value to encrypt
     * @return The encrypted string
     * @throws GeneralSecurityException
     *             In case of issue with the encryption
     * @throws UnsupportedEncodingException
     *             In case of issue with the charset conversion
     */
    public static String encrypt(String value) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM_DETAILS, "SunJCE");
        byte[] iv = new byte[IV_BLOCK_SIZE_IN_BYTES];
        SecureRandom.getInstance("SHA1PRNG").nextBytes(iv);
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY_SPEC, ivspec);
        return Hex.encodeHexString(ArrayUtils.addAll(iv, cipher.doFinal(value.getBytes(Charsets.UTF_8))));
    }

    /**
     * Decrypt a value based on the Clamp Encryption Key.
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
    public static String decrypt(String message) throws GeneralSecurityException, DecoderException {
        byte[] encryptedMessage = Hex.decodeHex(message.toCharArray());
        Cipher cipher = Cipher.getInstance(ALGORITHM_DETAILS, "SunJCE");
        IvParameterSpec ivspec = new IvParameterSpec(ArrayUtils.subarray(encryptedMessage, 0, IV_BLOCK_SIZE_IN_BYTES));
        byte[] realData = ArrayUtils.subarray(encryptedMessage, IV_BLOCK_SIZE_IN_BYTES, encryptedMessage.length);
        cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY_SPEC, ivspec);
        byte[] decrypted = cipher.doFinal(realData);
        return new String(decrypted);
    }

    /**
     * Method used to generate the SecretKeySpec from a Base64 String.
     * 
     * @param keyString
     *            The key as a string in Base 64
     * @return The SecretKeySpec created
     * @throws DecoderException
     *             In case of issues with the decoding of Base64
     */
    private static SecretKeySpec getSecretKeySpec(String keyString) throws DecoderException {
        byte[] key = Hex.decodeHex(keyString.toCharArray());
        return new SecretKeySpec(key, ALGORITHM);
    }

    /**
     * Reads SecretKeySpec from file specified by propertiesFileName
     *
     * @param propertiesFileName
     *            File name with properties
     * @return SecretKeySpec secret key spec read from propertiesFileName
     */
    private static SecretKeySpec readSecretKeySpec(String propertiesFileName) {
        Properties props = new Properties();
        try {
        	//Workaround fix to make encryption key configurable.
        	//System environment variable takes precedence for over clds/key.properties
        	String encryptionKey = System.getenv(AES_ENCRYPTION_KEY);
        	if(encryptionKey != null && encryptionKey.trim().length() > 0) {
        		return getSecretKeySpec(encryptionKey);
        	} else {
        		props.load(ResourceFileUtil.getResourceAsStream(propertiesFileName));
                return getSecretKeySpec(props.getProperty(KEY_PARAM));
        	}
        } catch (IOException | DecoderException e) {
            logger.error("Exception occurred during the key reading", e);
            return null;
        }
    }
}

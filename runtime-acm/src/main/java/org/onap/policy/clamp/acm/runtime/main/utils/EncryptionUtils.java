/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation.
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

import jakarta.ws.rs.core.Response;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.springframework.stereotype.Component;

/**
 * Class to encrypt/decrypt sensitive fields in the database.
 */

@Component
public class EncryptionUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String PBK_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String MARKER = "ENCRYPTED:";
    private static final String SENSITIVE_METADATA = "sensitive";
    private static final int GCM_TAG = 128;
    private static final int IV_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final String passPhrase;
    private final String salt;


    private static byte[] generateIV() {
        var iv = new byte[IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv); //random iv
        return iv;
    }

    /**
     * Constructor EncryptionUtils.
     * @param acRuntimeParameterGroup acRuntimeParameterGroup
     */
    public EncryptionUtils(AcRuntimeParameterGroup acRuntimeParameterGroup) {
        this.passPhrase = acRuntimeParameterGroup.getAcmParameters().getPassPhrase();
        this.salt = acRuntimeParameterGroup.getAcmParameters().getSalt();
    }

    /**
     * Check encryption is enabled.
     * @return boolean result
     */
    public boolean encryptionEnabled() {
        return passPhrase != null && salt != null;
    }


    /**
     * Find and encrypt sensitive fields in an AC instance.
     * @param acDefinition acDefinition
     * @param automationComposition acInstance
     */
    public void findAndEncryptSensitiveData(AutomationCompositionDefinition acDefinition,
                                            AutomationComposition automationComposition) {
        try {
            for (var acInstanceElement: automationComposition.getElements().values()) {
                var sensitiveProperties = findSensitiveElementFields(acDefinition, acInstanceElement);
                for (var property : sensitiveProperties) {
                    var elementProperties = acInstanceElement.getProperties();
                    var sensitiveVal = elementProperties.get(property.getName());
                    if (sensitiveVal instanceof String sensitiveStr && !sensitiveStr.startsWith(MARKER)) {
                        var encryptedVal = encrypt(sensitiveStr);
                        elementProperties.put(property.getName(), encryptedVal);
                    }
                }
            }
        } catch (Exception e) {
            throw new AutomationCompositionRuntimeException(Response.Status.fromStatusCode(500),
                    "Failed to encrypt instance field ", e);
        }
    }


    /**
     * Find and decrypt sensitive fields in an AC instance.
     * @param automationComposition acInstance
     */
    public void findAndDecryptSensitiveData(AutomationComposition automationComposition) {
        try {
            for (var acInstanceElement: automationComposition.getElements().values()) {
                for (var property : acInstanceElement.getProperties().entrySet()) {
                    var propertyVal = property.getValue();
                    if (propertyVal instanceof String propertyValStr && propertyValStr.startsWith(MARKER)) {
                        var decryptedVal = decrypt(propertyValStr);
                        acInstanceElement.getProperties().put(property.getKey(), decryptedVal);
                    }
                }
            }
        } catch (Exception e) {
            throw new AutomationCompositionRuntimeException(Response.Status.fromStatusCode(500),
                    "Failed to decrypt instance field ", e);
        }
    }


    private List<ToscaProperty> findSensitiveElementFields(AutomationCompositionDefinition acDefinition,
                                                           AutomationCompositionElement acInstanceElement) {

        List<ToscaProperty> sensitiveProperties = new ArrayList<>();

        // Fetch the node template element
        var acDefElementOpt = acDefinition.getServiceTemplate().getToscaTopologyTemplate().getNodeTemplates()
                .values().stream().filter(acDefElement -> acDefElement.getName()
                        .equals(acInstanceElement.getDefinition().getName())).findFirst();

        // Fetch node type
        if (acDefElementOpt.isPresent()) {
            var toscaNodeTypeOpt = acDefinition.getServiceTemplate().getNodeTypes().values().stream()
                    .filter(toscaNodeType -> toscaNodeType.getName()
                            .equals(acDefElementOpt.get().getType())).findFirst();

            toscaNodeTypeOpt.ifPresent(toscaNodeType -> toscaNodeType.getProperties().values()
                    .stream().filter(property -> property.getMetadata() != null
                            && property.getMetadata().containsKey(SENSITIVE_METADATA))
                    .forEach(sensitiveProperties::add));
        }
        return sensitiveProperties;
    }


    private SecretKey getSecretKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        var factory = SecretKeyFactory.getInstance(PBK_ALGORITHM);
        var spec = new PBEKeySpec(passPhrase.toCharArray(), salt.getBytes(), 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private String encrypt(String input) throws IllegalBlockSizeException, BadPaddingException,
            NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException,
            NoSuchPaddingException {
        var iv = generateIV();
        var parameterSpec = new GCMParameterSpec(GCM_TAG, iv);
        var cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec);
        var cipherText = cipher.doFinal(input.getBytes());
        var cipherByte = ByteBuffer.allocate(iv.length + cipherText.length).put(iv).put(cipherText).array();
        return MARKER + Base64.getEncoder().encodeToString(cipherByte);
    }

    private String decrypt(String cipherText) throws IllegalBlockSizeException, BadPaddingException,
            NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException {
        var decodedText = Base64.getDecoder().decode(cipherText.substring(MARKER.length()).getBytes());
        var byteBuffer = ByteBuffer.wrap(decodedText);
        var iv = new byte[IV_LENGTH];
        byteBuffer.get(iv);
        var encryptedByte = new byte[byteBuffer.remaining()];
        byteBuffer.get(encryptedByte);

        var parameterSpec = new GCMParameterSpec(GCM_TAG, iv);
        var cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), parameterSpec);
        var plainText = cipher.doFinal(encryptedByte);
        return new String(plainText);
    }
}

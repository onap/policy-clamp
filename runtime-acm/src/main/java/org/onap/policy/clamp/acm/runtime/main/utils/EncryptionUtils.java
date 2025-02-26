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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaSchemaDefinition;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final boolean encryptionEnabled;

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionUtils.class);


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
        this.encryptionEnabled = acRuntimeParameterGroup.getAcmParameters().isEnableEncryption();
    }

    /**
     * Check encryption is enabled.
     * @return boolean result
     */
    public boolean encryptionEnabled() {
        return encryptionEnabled;
    }


    /**
     * Find and encrypt sensitive fields in an AC instance.
     * @param acDefinition acDefinition
     * @param automationComposition acInstance
     */
    public void findAndEncryptSensitiveData(AutomationCompositionDefinition acDefinition,
                                            AutomationComposition automationComposition) {
        try {
            var acNodeTypes = Optional.ofNullable(acDefinition.getServiceTemplate().getNodeTypes()).map(Map::values)
                    .orElse(Collections.emptyList());
            var acDataTypes = Optional.ofNullable(acDefinition.getServiceTemplate().getDataTypes()).map(Map::values)
                    .orElse(Collections.emptyList());
            var nodeTemplates = Optional.ofNullable(acDefinition.getServiceTemplate().getToscaTopologyTemplate())
                    .map(ToscaTopologyTemplate::getNodeTemplates)
                    .map(Map::values).orElse(Collections.emptyList());

            for (var acInstanceElement: automationComposition.getElements().values()) {
                var sensitiveProperties = filterSensitiveProperties(acInstanceElement, acNodeTypes, acDataTypes,
                        nodeTemplates);
                LOGGER.debug("Sensitive properties for the element {} : {}",
                        acInstanceElement.getId(), sensitiveProperties);
                for (var property : sensitiveProperties) {
                    var elementProperties = acInstanceElement.getProperties();
                    var sensitiveVal = elementProperties.get(property.getName());
                    if (sensitiveVal == null) {
                        encryptNested(property, elementProperties);
                    } else if (sensitiveVal instanceof String sensitiveStr && !sensitiveStr.startsWith(MARKER)) {
                        var encryptedVal = encrypt(sensitiveStr);
                        elementProperties.put(property.getName(), encryptedVal);
                        LOGGER.debug("Property {} is successfully encrypted", property.getName());
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
                        LOGGER.debug("Property {} is successfully decrypted", property.getKey());
                    } else {
                        decryptNested(propertyVal);
                    }
                }
            }
        } catch (Exception e) {
            throw new AutomationCompositionRuntimeException(Response.Status.fromStatusCode(500),
                    "Failed to decrypt instance field ", e);
        }
    }

    private void decryptNested(Object propertyVal) throws InvalidAlgorithmParameterException, IllegalBlockSizeException,
            NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidKeyException {
        if (propertyVal instanceof List<?> listVal) {
            for (var listEntry : listVal) {
                if (listEntry instanceof Map<?, ?> tempMap) {
                    decryptNestedMap(tempMap);
                }
            }
        } else if (propertyVal instanceof Map<?, ?> tempMap) {
            decryptNestedMap(tempMap);
        }
    }

    private void decryptNestedMap(Map<?, ?> tempMap) throws InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException {
        @SuppressWarnings("unchecked")
        var nestedMap = (Map<Object, Object>) tempMap;
        for (var prop : nestedMap.entrySet()) {
            if (prop.getValue() instanceof String nestedStr && nestedStr.startsWith(MARKER)) {
                var encryptedVal = decrypt(nestedStr);
                nestedMap.put(prop.getKey(), encryptedVal);
                LOGGER.debug("Property {} is successfully decrypted", prop.getKey());
            }
        }
    }

    private void encryptNested(ToscaProperty property, Map<?, ?> properties)
            throws InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException,
            BadPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        // Iterate over nested maps to check if the property exists inside them
        for (var mapEntry : properties.entrySet()) {
            if (mapEntry.getValue() instanceof List<?> listVal) {
                for (var listEntry : listVal) {
                    if (listEntry instanceof Map<?, ?> tempMap) {
                        encryptNestedMaps(property, tempMap);
                    }
                }
            } else if (mapEntry.getValue() instanceof Map<?, ?> tempMap) {
                encryptNestedMaps(property, tempMap);
            }
        }

    }

    private void encryptNestedMaps(ToscaProperty property, Map<?, ?> tempMap) throws InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException {
        @SuppressWarnings("unchecked")
        var nestedMap = (Map<Object, Object>) tempMap;
        var nestedValue = nestedMap.get(property.getName());
        if (nestedValue instanceof String nestedStr && !nestedStr.startsWith(MARKER)) {
            var encryptedVal = encrypt(nestedStr);
            nestedMap.put(property.getName(), encryptedVal);
            LOGGER.debug("Property {} is successfully encrypted", property.getName());
        }
    }


    private List<ToscaProperty> filterSensitiveProperties(AutomationCompositionElement acInstanceElement,
                                                          Collection<ToscaNodeType> nodeTypes,
                                                          Collection<ToscaDataType> dataTypes,
                                                          Collection<ToscaNodeTemplate> nodeTemplates) {

        List<ToscaProperty> sensitiveProperties = new ArrayList<>();

        // Fetch the node template element
        var acDefElementOpt = nodeTemplates.stream().filter(acDefElement -> acDefElement.getName()
                        .equals(acInstanceElement.getDefinition().getName())).findFirst();

        // Fetch node type
        if (acDefElementOpt.isPresent()) {
            var toscaNodeTypeOpt = nodeTypes.stream().filter(toscaNodeType -> toscaNodeType.getName()
                            .equals(acDefElementOpt.get().getType())).findFirst();

            if (toscaNodeTypeOpt.isPresent()) {
                toscaNodeTypeOpt.get().getProperties().values().stream()
                        .filter(this::isSensitiveMetadata)
                        .forEach(sensitiveProperties::add);

                for (var property : toscaNodeTypeOpt.get().getProperties().values()) {
                    dataTypes.stream()
                            .filter(datatype -> isDataTypeRef(property, datatype))
                            .flatMap(dataType -> dataType.getProperties().values().stream())
                            .filter(this::isSensitiveMetadata)
                            .forEach(sensitiveProperties::add);
                }
            }
        }
        return sensitiveProperties;
    }

    private boolean isSensitiveMetadata(ToscaProperty property) {
        if (property.getMetadata() == null) {
            return false;
        }
        var metadataValue = property.getMetadata().get(SENSITIVE_METADATA);
        return "true".equals(metadataValue);
    }

    private boolean isDataTypeRef(ToscaProperty property, ToscaDataType dataType) {
        var dataTypeName = dataType.getDefinedName();
        var propertyEntity = Optional.ofNullable(property.getEntrySchema()).map(ToscaSchemaDefinition::getType);
        return dataTypeName.equals(property.getType()) || dataTypeName.equals(propertyEntity.orElse(null));
    }


    private SecretKey getSecretKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        var factory = SecretKeyFactory.getInstance(PBK_ALGORITHM);
        var salt = "salt";
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

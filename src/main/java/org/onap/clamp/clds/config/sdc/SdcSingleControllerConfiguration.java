/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.config.sdc;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import com.google.gson.JsonObject;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.onap.clamp.clds.exception.sdc.controller.SdcParametersException;
import org.onap.clamp.clds.util.CryptoUtils;
import org.onap.sdc.api.consumer.IConfiguration;

/**
 * This class maps the SDC config JSON for one controller.
 */
public class SdcSingleControllerConfiguration implements IConfiguration {

    private static final EELFLogger logger = EELFManager.getInstance()
            .getLogger(SdcSingleControllerConfiguration.class);
    /**
     * The sdc Controller name corresponding.
     */
    private String sdcControllerName;
    /**
     * The root of the JSON.
     */
    private JsonObject jsonRootNode;
    // All keys that can be present in the JSON
    public static final String CONSUMER_GROUP_ATTRIBUTE_NAME = "consumerGroup";
    public static final String CONSUMER_ID_ATTRIBUTE_NAME = "consumerId";
    public static final String ENVIRONMENT_NAME_ATTRIBUTE_NAME = "environmentName";
    public static final String SDC_KEY_ATTRIBUTE_NAME = "password";
    public static final String POLLING_INTERVAL_ATTRIBUTE_NAME = "pollingInterval";
    public static final String RELEVANT_ARTIFACT_TYPES_ATTRIBUTE_NAME = "relevantArtifactTypes";
    public static final String USER_ATTRIBUTE_NAME = "user";
    public static final String SDC_ADDRESS_ATTRIBUTE_NAME = "sdcAddress";
    public static final String POLLING_TIMEOUT_ATTRIBUTE_NAME = "pollingTimeout";
    public static final String ACTIVATE_SERVER_TLS_AUTH = "activateServerTLSAuth";
    public static final String KEY_STORE_KEY = "keyStorePassword";
    public static final String KEY_STORE_PATH = "keyStorePath";
    public static final String MESSAGE_BUS_ADDRESSES = "messageBusAddresses";
    private String errorMessageKeyNotFound;
    /**
     * Supported artifact types.
     */
    public static final String HEAT = "HEAT";
    public static final String HEAT_ARTIFACT = "HEAT_ARTIFACT";
    public static final String HEAT_ENV = "HEAT_ENV";
    public static final String HEAT_NESTED = "HEAT_NESTED";
    public static final String HEAT_NET = "HEAT_NET";
    public static final String HEAT_VOL = "HEAT_VOL";
    public static final String OTHER = "OTHER";
    public static final String TOSCA_CSAR = "TOSCA_CSAR";
    public static final String VF_MODULES_METADATA = "VF_MODULES_METADATA";
    private static final String[] SUPPORTED_ARTIFACT_TYPES = {
            TOSCA_CSAR, VF_MODULES_METADATA
    };
    public static final List<String> SUPPORTED_ARTIFACT_TYPES_LIST = Collections
            .unmodifiableList(Arrays.asList(SUPPORTED_ARTIFACT_TYPES));

    /**
     * This constructor builds a SdcSingleControllerConfiguration from the
     * corresponding json.
     * 
     * @param jsonNode
     *            The JSON node
     * @param controllerName
     *            The controller name that must appear in the JSON
     */
    public SdcSingleControllerConfiguration(JsonObject jsonNode, String controllerName) {
        jsonRootNode = jsonNode;
        setSdcControllerName(controllerName);
        testAllRequiredParameters();
    }

    public String getSdcControllerName() {
        return sdcControllerName;
    }

    public void setSdcControllerName(String controllerName) {
        this.sdcControllerName = controllerName;
        errorMessageKeyNotFound = " parameter cannot be found in config file for controller name" + sdcControllerName;
        testAllRequiredParameters();
    }

    private String getStringConfig(String key) {
        if (jsonRootNode != null && jsonRootNode.get(key) != null) {
            String config = jsonRootNode.get(key).getAsString();
            return config.isEmpty() ? null : config;
        }
        return null;
    }

    private Integer getIntConfig(String key) {
        if (jsonRootNode != null && jsonRootNode.get(key) != null) {
            return jsonRootNode.get(key).getAsInt();
        } else {
            return 0;
        }
    }

    private String getEncryptedStringConfig(String key) throws GeneralSecurityException, DecoderException {
        if (jsonRootNode != null && jsonRootNode.get(key) != null) {
            return jsonRootNode.get(key).getAsString().isEmpty() ? null
                    : CryptoUtils.decrypt(jsonRootNode.get(key).getAsString());
        }
        return null;
    }

    @Override
    public java.lang.Boolean isUseHttpsWithDmaap() {
        return false;
    }

    @Override
    public String getConsumerGroup() {
        if (jsonRootNode != null && jsonRootNode.get(CONSUMER_GROUP_ATTRIBUTE_NAME) != null) {
            String config = jsonRootNode.get(CONSUMER_GROUP_ATTRIBUTE_NAME).getAsString();
            return "NULL".equals(config) || config.isEmpty() ? null : config;
        }
        return null;
    }

    @Override
    public String getConsumerID() {
        return getStringConfig(CONSUMER_ID_ATTRIBUTE_NAME);
    }

    @Override
    public String getEnvironmentName() {
        return getStringConfig(ENVIRONMENT_NAME_ATTRIBUTE_NAME);
    }

    @Override
    public String getPassword() {
        try {
            return getEncryptedStringConfig(SDC_KEY_ATTRIBUTE_NAME);
        } catch (GeneralSecurityException | DecoderException e) {
            logger.error("Unable to decrypt the SDC password", e);
            return null;
        }
    }

    @Override
    public int getPollingInterval() {
        return getIntConfig(POLLING_INTERVAL_ATTRIBUTE_NAME);
    }

    @Override
    public List<String> getRelevantArtifactTypes() {
        // DO not return the Static List SUPPORTED_ARTIFACT_TYPES_LIST because
        // the ASDC Client could try to modify it !!!
        return Arrays.asList(SUPPORTED_ARTIFACT_TYPES);
    }

    @Override
    public String getUser() {
        return getStringConfig(USER_ATTRIBUTE_NAME);
    }

    @Override
    public String getAsdcAddress() {
        return getStringConfig(SDC_ADDRESS_ATTRIBUTE_NAME);
    }

    @Override
    public int getPollingTimeout() {
        return getIntConfig(POLLING_TIMEOUT_ATTRIBUTE_NAME);
    }

    @Override
    public boolean activateServerTLSAuth() {
        if (jsonRootNode != null && jsonRootNode.get(ACTIVATE_SERVER_TLS_AUTH) != null && jsonRootNode.get(ACTIVATE_SERVER_TLS_AUTH).isJsonPrimitive()) {
            return jsonRootNode.get(ACTIVATE_SERVER_TLS_AUTH).getAsBoolean();
        } else {
            return false;
        }
    }

    @Override
    public String getKeyStorePassword() {
        try {
            return getEncryptedStringConfig(KEY_STORE_KEY);
        } catch (GeneralSecurityException | DecoderException e) {
            logger.error("Unable to decrypt the SDC password", e);
            return null;
        }
    }

    @Override
    public String getKeyStorePath() {
        return getStringConfig(KEY_STORE_PATH);
    }

    /**
     * This method can be used to validate all required parameters are well
     * there.
     */
    public void testAllRequiredParameters() {
        // Special case for this attribute that can be null from
        // getConsumerGroup
        if (jsonRootNode == null) {
            throw new SdcParametersException("Json is null for controller " + this.getSdcControllerName());
        }
        if (this.getConsumerGroup() == null && (jsonRootNode.get(CONSUMER_GROUP_ATTRIBUTE_NAME) == null
                || !"NULL".equals(jsonRootNode.get(CONSUMER_GROUP_ATTRIBUTE_NAME).getAsString()))) {
            throw new SdcParametersException(CONSUMER_GROUP_ATTRIBUTE_NAME + errorMessageKeyNotFound);
        }
        if (this.getConsumerID() == null || this.getConsumerID().isEmpty()) {
            throw new SdcParametersException(CONSUMER_ID_ATTRIBUTE_NAME + errorMessageKeyNotFound);
        }
        if (this.getEnvironmentName() == null || this.getEnvironmentName().isEmpty()) {
            throw new SdcParametersException(ENVIRONMENT_NAME_ATTRIBUTE_NAME + errorMessageKeyNotFound);
        }
        if (this.getAsdcAddress() == null || this.getAsdcAddress().isEmpty()) {
            throw new SdcParametersException(SDC_ADDRESS_ATTRIBUTE_NAME + errorMessageKeyNotFound);
        }
        if (this.getMsgBusAddress() == null || this.getMsgBusAddress().isEmpty()) {
            throw new SdcParametersException(MESSAGE_BUS_ADDRESSES + errorMessageKeyNotFound);
        }
        if (this.getPassword() == null || this.getPassword().isEmpty()) {
            throw new SdcParametersException(SDC_KEY_ATTRIBUTE_NAME + errorMessageKeyNotFound);
        }
        if (this.getPollingInterval() == 0) {
            throw new SdcParametersException(POLLING_INTERVAL_ATTRIBUTE_NAME + errorMessageKeyNotFound);
        }
        if (this.getPollingTimeout() == 0) {
            throw new SdcParametersException(POLLING_TIMEOUT_ATTRIBUTE_NAME + errorMessageKeyNotFound);
        }
        if (this.getRelevantArtifactTypes() == null || this.getRelevantArtifactTypes().isEmpty()) {
            throw new SdcParametersException(RELEVANT_ARTIFACT_TYPES_ATTRIBUTE_NAME + errorMessageKeyNotFound);
        }
        if (this.getUser() == null || this.getUser().isEmpty()) {
            throw new SdcParametersException(USER_ATTRIBUTE_NAME + errorMessageKeyNotFound);
        }
    }

    /**
     * The flag allows the client to receive metadata for all resources of the
     * service regardless of the artifacts associated to them. Setting the flag
     * to false will preserve legacy behavior.
     */
    @Override
    public boolean isFilterInEmptyResources() {
        return false;
    }

    @Override
    public List<String> getMsgBusAddress() {
        List<String> addressesList = new ArrayList<>();
        if (jsonRootNode != null && jsonRootNode.get(MESSAGE_BUS_ADDRESSES) != null) {
            jsonRootNode.get(MESSAGE_BUS_ADDRESSES).getAsJsonArray().forEach(k -> addressesList.add(k.getAsString()));
            return addressesList;
        } else {
            return addressesList;
        }
    }
}

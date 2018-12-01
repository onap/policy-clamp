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

package org.onap.clamp.clds.client.req.policy;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.config.PolicyConfiguration;
import org.onap.clamp.clds.exception.policy.PolicyClientException;
import org.onap.clamp.clds.model.CldsToscaModel;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.PolicyItem;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.policy.api.AttributeType;
import org.onap.policy.api.ConfigRequestParameters;
import org.onap.policy.api.DeletePolicyCondition;
import org.onap.policy.api.DeletePolicyParameters;
import org.onap.policy.api.DictionaryType;
import org.onap.policy.api.ImportParameters;
import org.onap.policy.api.ImportParameters.IMPORT_TYPE;
import org.onap.policy.api.PolicyChangeResponse;
import org.onap.policy.api.PolicyClass;
import org.onap.policy.api.PolicyConfigException;
import org.onap.policy.api.PolicyConfigType;
import org.onap.policy.api.PolicyEngine;
import org.onap.policy.api.PolicyEngineException;
import org.onap.policy.api.PolicyParameters;
import org.onap.policy.api.PolicyType;
import org.onap.policy.api.PushPolicyParameters;
import org.onap.policy.api.RuleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Policy utility methods - specifically, send the policy.
 */
@Component
@Primary
public class PolicyClient {

    protected PolicyEngine policyEngine;
    protected static final String LOG_POLICY_PREFIX = "Response is ";
    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(PolicyClient.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    public static final String POLICY_MSTYPE_PROPERTY_NAME = "policy.ms.type";
    public static final String POLICY_ONAPNAME_PROPERTY_NAME = "policy.onap.name";
    public static final String POLICY_BASENAME_PREFIX_PROPERTY_NAME = "policy.base.policyNamePrefix";
    public static final String POLICY_OP_NAME_PREFIX_PROPERTY_NAME = "policy.op.policyNamePrefix";
    public static final String POLICY_MS_NAME_PREFIX_PROPERTY_NAME = "policy.ms.policyNamePrefix";
    public static final String POLICY_OP_TYPE_PROPERTY_NAME = "policy.op.type";
    public static final String POLICY_GUARD_SUFFIX = "_Guard";
    public static final String TOSCA_FILE_TEMP_PATH = "tosca.filePath";

    @Autowired
    protected ApplicationContext appContext;
    @Autowired
    protected ClampProperties refProp;
    @Autowired
    private PolicyConfiguration policyConfiguration;

    /**
     * Perform Guard policy type.
     *
     * @param attributes
     *        A map of attributes
     * @param prop
     *        The ModelProperties
     * @param policyRequestUuid
     *        PolicyRequest UUID
     * @return The response message of policy
     */
    public String sendGuardPolicy(Map<AttributeType, Map<String, String>> attributes, ModelProperties prop,
        String policyRequestUuid, PolicyItem policyItem) {
        PolicyParameters policyParameters = new PolicyParameters();
        // Set Policy Type(Mandatory)
        policyParameters.setPolicyClass(PolicyClass.Decision);
        // Set Policy Name(Mandatory)
        policyParameters.setPolicyName(prop.getPolicyScopeAndNameWithUniqueGuardId());
        // documentation says this is options, but when tested, got the
        // following failure: java.lang.Exception: Policy send failed: PE300 -
        // Data Issue: No policyDescription given.
        policyParameters.setPolicyDescription(refProp.getStringValue("op.policyDescription"));
        policyParameters.setOnapName("PDPD");
        policyParameters.setRuleProvider(RuleProvider.valueOf(policyItem.getGuardPolicyType()));
        policyParameters.setAttributes(attributes);
        // Set a random UUID(Mandatory)
        policyParameters.setRequestID(UUID.fromString(policyRequestUuid));

        String rtnMsg = send(policyParameters, prop, null);
        push(DictionaryType.Decision.toString(), prop);
        return rtnMsg;
    }

    /**
     * Perform BRMS policy type.
     *
     * @param attributes
     *        A map of attributes
     * @param prop
     *        The ModelProperties
     * @param policyRequestUuid
     *        PolicyRequest UUID
     * @return The response message of policy
     */
    public String sendBrmsPolicy(Map<AttributeType, Map<String, String>> attributes, ModelProperties prop,
        String policyRequestUuid) {
        PolicyParameters policyParameters = new PolicyParameters();
        // Set Policy Type(Mandatory)
        policyParameters.setPolicyConfigType(PolicyConfigType.BRMS_PARAM);
        // Set Policy Name(Mandatory)
        policyParameters.setPolicyName(prop.getPolicyScopeAndNameWithUniqueId());
        // documentation says this is options, but when tested, got the
        // following failure: java.lang.Exception: Policy send failed: PE300 -
        // Data Issue: No policyDescription given.
        policyParameters.setPolicyDescription(refProp.getStringValue("op.policyDescription"));
        policyParameters.setAttributes(attributes);
        // Set a random UUID(Mandatory)
        policyParameters.setRequestID(UUID.fromString(policyRequestUuid));
        String policyNamePrefix = refProp.getStringValue(POLICY_OP_NAME_PREFIX_PROPERTY_NAME);
        String rtnMsg = send(policyParameters, prop, policyNamePrefix);
        String policyType = refProp.getStringValue(POLICY_OP_TYPE_PROPERTY_NAME);
        push(policyType, prop);
        return rtnMsg;
    }

    /**
     * Perform send of microservice policy in JSON.
     *
     * @param policyJson
     *        The policy JSON
     * @param prop
     *        The ModelProperties
     * @param policyRequestUuid
     *        The policy Request UUID
     * @return The response message of policy
     */
    public String sendMicroServiceInJson(String policyJson, ModelProperties prop, String policyRequestUuid) {
        PolicyParameters policyParameters = new PolicyParameters();
        // Set Policy Type
        policyParameters.setPolicyConfigType(PolicyConfigType.MicroService);
        policyParameters.setOnapName(refProp.getStringValue(POLICY_ONAPNAME_PROPERTY_NAME));
        policyParameters.setPolicyName(prop.getCurrentPolicyScopeAndPolicyName());
        policyParameters.setConfigBody(policyJson);
        policyParameters.setConfigBodyType(PolicyType.JSON);
        policyParameters.setRequestID(UUID.fromString(policyRequestUuid));
        String policyNamePrefix = refProp.getStringValue(POLICY_MS_NAME_PREFIX_PROPERTY_NAME);
        // Adding this line to clear the policy id from policy name while
        // pushing to policy engine
        prop.setPolicyUniqueId("");
        String rtnMsg = send(policyParameters, prop, policyNamePrefix);
        String policyType = refProp.getStringValue(POLICY_MSTYPE_PROPERTY_NAME);
        push(policyType, prop);
        return rtnMsg;
    }

    /**
     * Perform send of base policy in OTHER type.
     *
     * @param configBody
     *        The config policy string body
     * @param configPolicyName
     *        The config policy name of the component that has been pre-deployed in
     *        DCAE
     * @param prop
     *        The ModelProperties
     * @param policyRequestUuid
     *        The policy request UUID
     * @return The answer from policy call
     */
    public String sendBasePolicyInOther(String configBody, String configPolicyName, ModelProperties prop,
        String policyRequestUuid) {
        PolicyParameters policyParameters = new PolicyParameters();
        // Set Policy Type
        policyParameters.setPolicyConfigType(PolicyConfigType.Base);
        policyParameters.setOnapName(refProp.getStringValue(POLICY_ONAPNAME_PROPERTY_NAME));
        policyParameters.setPolicyName(prop.getCurrentPolicyScopeAndPolicyName());
        policyParameters.setConfigBody(configBody);
        policyParameters.setConfigBodyType(PolicyType.OTHER);
        policyParameters.setConfigName("HolmesPolicy");
        policyParameters.setPolicyName(configPolicyName);
        policyParameters.setRequestID(UUID.fromString(policyRequestUuid));
        // Adding this line to clear the policy id from policy name while
        // pushing to policy engine
        prop.setPolicyUniqueId("");
        String rtnMsg = send(policyParameters, prop, refProp.getStringValue(POLICY_BASENAME_PREFIX_PROPERTY_NAME));
        push(PolicyConfigType.Base.toString(), prop);
        return rtnMsg;
    }

    /**
     * Perform send of Microservice policy in OTHER type.
     *
     * @param configBody
     *        The config policy string body
     * @param prop
     *        The ModelProperties
     * @return The answer from policy call
     */
    public String sendMicroServiceInOther(String configBody, ModelProperties prop) {
        PolicyParameters policyParameters = new PolicyParameters();
        // Set Policy Type
        policyParameters.setPolicyConfigType(PolicyConfigType.MicroService);
        policyParameters.setOnapName(refProp.getStringValue(POLICY_ONAPNAME_PROPERTY_NAME));
        policyParameters.setPolicyName(prop.getCurrentPolicyScopeAndPolicyName());
        policyParameters.setConfigBody(configBody);
        String policyNamePrefix = refProp.getStringValue(POLICY_MS_NAME_PREFIX_PROPERTY_NAME);
        // Adding this line to clear the policy id from policy name while
        // pushing to policy engine
        prop.setPolicyUniqueId("");
        String rtnMsg = send(policyParameters, prop, policyNamePrefix);
        String policyType = refProp.getStringValue(POLICY_MSTYPE_PROPERTY_NAME);
        push(policyType, prop);
        return rtnMsg;
    }

    /**
     * Perform send of Configuration or Decision policies.
     *
     * @param policyParameters
     *        The PolicyParameters
     * @param prop
     *        The ModelProperties
     * @return The response message of Policy
     */
    protected String send(PolicyParameters policyParameters, ModelProperties prop, String policyNamePrefix) {
        // Verify whether it is triggered by Validation Test button from UI
        if (prop.isTestOnly()) {
            return "send not executed for test action";
        }
        // API method to create or update Policy.
        PolicyChangeResponse response = null;
        String responseMessage = "";
        Date startTime = new Date();
        try {
            if ((PolicyClass.Decision.equals(policyParameters.getPolicyClass()) && !checkDecisionPolicyExists(prop))
                || (PolicyClass.Config.equals(policyParameters.getPolicyClass())
                    && !checkPolicyExists(policyNamePrefix, prop))) {
                LoggingUtils.setTargetContext("Policy", "createPolicy");
                logger.info("Attempting to create policy for action=" + prop.getActionCd());
                response = getPolicyEngine().createPolicy(policyParameters);
                responseMessage = response.getResponseMessage();
            } else {
                LoggingUtils.setTargetContext("Policy", "updatePolicy");
                logger.info("Attempting to update policy for action=" + prop.getActionCd());
                response = getPolicyEngine().updatePolicy(policyParameters);
                responseMessage = response.getResponseMessage();
            }
        } catch (Exception e) {
            LoggingUtils.setResponseContext("900", "Policy send failed", this.getClass().getName());
            LoggingUtils.setErrorContext("900", "Policy send error");
            logger.error("Exception occurred during policy communication", e);
            throw new PolicyClientException("Exception while communicating with Policy", e);
        }
        logger.info(LOG_POLICY_PREFIX + responseMessage);
        LoggingUtils.setTimeContext(startTime, new Date());
        if (response.getResponseCode() == 200) {
            LoggingUtils.setResponseContext("0", "Policy send success", this.getClass().getName());
            logger.info("Policy send successful");
            metricsLogger.info("Policy send success");
        } else {
            LoggingUtils.setResponseContext("900", "Policy send failed", this.getClass().getName());
            logger.warn("Policy send failed: " + responseMessage);
            metricsLogger.info("Policy send failure");
            throw new BadRequestException("Policy send failed: " + responseMessage);
        }
        return responseMessage;
    }

    /**
     * Format and send push of policy.
     *
     * @param policyType
     *        The policy Type
     * @param prop
     *        The ModelProperties
     * @return The response message of policy
     */
    protected String push(String policyType, ModelProperties prop) {
        // Verify whether it is triggered by Validation Test button from UI
        if (prop.isTestOnly()) {
            return "push not executed for test action";
        }
        PushPolicyParameters pushPolicyParameters = new PushPolicyParameters();
        // Parameter arguments
        if (prop.getPolicyUniqueId() != null && !prop.getPolicyUniqueId().isEmpty()) {
            if (DictionaryType.Decision.toString().equals(policyType)) {
                pushPolicyParameters.setPolicyName(prop.getPolicyScopeAndNameWithUniqueGuardId());
            } else {
                pushPolicyParameters.setPolicyName(prop.getPolicyScopeAndNameWithUniqueId());
            }
        } else {
            pushPolicyParameters.setPolicyName(prop.getCurrentPolicyScopeAndPolicyName());
        }
        logger.info("Policy Name in Push policy method - " + pushPolicyParameters.getPolicyName());
        pushPolicyParameters.setPolicyType(policyType);
        pushPolicyParameters.setPdpGroup(refProp.getStringValue("policy.pdp.group"));
        pushPolicyParameters.setRequestID(null);
        // API method to create or update Policy.
        PolicyChangeResponse response;
        String responseMessage = "";
        try {
            LoggingUtils.setTargetContext("Policy", "pushPolicy");
            logger.info("Attempting to push policy...");
            response = getPolicyEngine().pushPolicy(pushPolicyParameters);
            if (response != null) {
                responseMessage = response.getResponseMessage();
            }
        } catch (Exception e) {
            LoggingUtils.setResponseContext("900", "Policy push failed", this.getClass().getName());
            LoggingUtils.setErrorContext("900", "Policy push error");
            logger.error("Exception occurred during policy communication", e);
            throw new PolicyClientException("Exception while communicating with Policy", e);
        }
        logger.info(LOG_POLICY_PREFIX + responseMessage);
        if (response != null && (response.getResponseCode() == 200 || response.getResponseCode() == 204)) {
            LoggingUtils.setResponseContext("0", "Policy push success", this.getClass().getName());
            logger.info("Policy push successful");
            metricsLogger.info("Policy push success");
        } else {
            LoggingUtils.setResponseContext("900", "Policy push failed", this.getClass().getName());
            logger.warn("Policy push failed: " + responseMessage);
            metricsLogger.info("Policy push failure");
            throw new BadRequestException("Policy push failed: " + responseMessage);
        }
        return responseMessage;
    }

    /**
     * Use list Decision policy to know if the decision policy exists.
     *
     * @param prop
     *        The model properties
     * @return true if it exists, false otherwise
     */
    protected boolean checkDecisionPolicyExists(ModelProperties prop) {
        boolean policyexists = false;

        logger.info("Search in Policy Engine for DecisionpolicyName=" + prop.getPolicyScopeAndNameWithUniqueGuardId());
        try {
            // No other choice than pushing to see if it exists or not
            String response = push(DictionaryType.Decision.toString(), prop);
            if (response != null) {
                policyexists = true;
            }
        } catch (BadRequestException e) {
            // just print warning - if no policy version found
            logger.warn("Policy not found...policy name - " + prop.getPolicyScopeAndNameWithUniqueGuardId(), e);
        }
        return policyexists;
    }

    /**
     * Use list Policy API to retrieve the policy. Return true if policy exists
     * otherwise return false.
     *
     * @param policyNamePrefix
     *        The Policy Name Prefix
     * @param prop
     *        The ModelProperties
     * @return The response message from policy
     * @throws PolicyConfigException
     *         In case of issues with policy engine
     */
    protected boolean checkPolicyExists(String policyNamePrefix, ModelProperties prop) {
        boolean policyexists = false;
        String policyName = "";
        try {

            if (prop.getPolicyUniqueId() != null && !prop.getPolicyUniqueId().isEmpty()) {
                policyName = prop.getCurrentPolicyScopeAndFullPolicyName(policyNamePrefix) + "_"
                    + prop.getPolicyUniqueId();
            } else {
                policyName = prop.getCurrentPolicyScopeAndFullPolicyName(policyNamePrefix);
            }
            logger.info("Search in Policy Engine for policyName=" + policyName);

            ConfigRequestParameters configRequestParameters = new ConfigRequestParameters();
            configRequestParameters.setPolicyName(policyName);
            Collection<String> response = getPolicyEngine().listConfig(configRequestParameters);
            if (response != null && !response.isEmpty() && !response.contains("Policy Name: null")) {
                policyexists = true;
            }
        } catch (PolicyConfigException e1) {
            // just print warning - if no policy version found
            logger.warn("Policy not found...policy name - " + policyName, e1);
        }
        return policyexists;
    }

    /**
     * This method create a new policy engine.
     *
     * @return A new policy engine
     */
    private synchronized PolicyEngine getPolicyEngine() {
        try {
            if (policyEngine == null) {
                policyEngine = new PolicyEngine(policyConfiguration.getProperties());
            }
        } catch (PolicyEngineException e) {
            throw new PolicyClientException("Exception when creating a new policy engine", e);
        }
        return policyEngine;
    }

    /**
     * Format and send delete Micro Service requests to Policy.
     *
     * @param prop
     *        The ModelProperties
     * @return The response message from Policy
     */
    public String deleteMicrosService(ModelProperties prop) {
        String deletePolicyResponse = "";
        try {
            String policyNamePrefix = refProp.getStringValue(POLICY_MS_NAME_PREFIX_PROPERTY_NAME);
            if (checkPolicyExists(policyNamePrefix, prop)) {
                String policyType = refProp.getStringValue(POLICY_MSTYPE_PROPERTY_NAME);
                deletePolicyResponse = deletePolicy(prop, policyType);
            }
        } catch (Exception e) {
            logger.error("Exception occurred during policy communication", e);
            throw new PolicyClientException("Exception while communicating with Policy", e);
        }
        return deletePolicyResponse;
    }

    /**
     * This method delete the Base policy.
     *
     * @param prop
     *        The model Properties
     * @return A string with the answer from policy
     */
    public String deleteBasePolicy(ModelProperties prop) {
        return deletePolicy(prop, PolicyConfigType.Base.toString());
    }

    /**
     * Format and send delete Guard requests to Policy.
     *
     * @param prop
     *        The ModelProperties
     * @return The response message from policy
     */
    public String deleteGuard(ModelProperties prop) {
        String deletePolicyResponse = "";
        try {

            if (checkDecisionPolicyExists(prop)) {
                deletePolicyResponse = deletePolicy(prop, DictionaryType.Decision.toString());
            }
        } catch (Exception e) {
            logger.error("Exception occurred during policy communication", e);
            throw new PolicyClientException("Exception while communicating with Policy", e);
        }
        return deletePolicyResponse;
    }

    /**
     * Format and send delete BRMS requests to Policy.
     *
     * @param prop
     *        The ModelProperties
     * @return The response message from policy
     */
    public String deleteBrms(ModelProperties prop) {
        String deletePolicyResponse = "";
        try {
            String policyNamePrefix = refProp.getStringValue(POLICY_OP_NAME_PREFIX_PROPERTY_NAME);
            if (checkPolicyExists(policyNamePrefix, prop)) {
                String policyType = refProp.getStringValue(POLICY_OP_TYPE_PROPERTY_NAME);
                deletePolicyResponse = deletePolicy(prop, policyType);
            }
        } catch (Exception e) {
            logger.error("Exception occurred during policy communication", e);
            throw new PolicyClientException("Exception while communicating with Policy", e);
        }
        return deletePolicyResponse;
    }

    /**
     * Format and send delete PAP and PDP requests to Policy.
     *
     * @param prop
     *        The ModelProperties
     * @return The response message from policy
     */
    protected String deletePolicy(ModelProperties prop, String policyType) {
        DeletePolicyParameters deletePolicyParameters = new DeletePolicyParameters();
        if (prop.getPolicyUniqueId() != null && !prop.getPolicyUniqueId().isEmpty()) {
            if (DictionaryType.Decision.toString().equals(policyType)) {
                deletePolicyParameters.setPolicyName(prop.getPolicyScopeAndNameWithUniqueGuardId());
            } else {
                deletePolicyParameters.setPolicyName(prop.getPolicyScopeAndNameWithUniqueId());
            }
        } else {
            deletePolicyParameters.setPolicyName(prop.getCurrentPolicyScopeAndPolicyName());
        }
        logger.info("Policy Name in delete policy method - " + deletePolicyParameters.getPolicyName());
        deletePolicyParameters.setPolicyComponent("PDP");
        deletePolicyParameters.setDeleteCondition(DeletePolicyCondition.ALL);
        deletePolicyParameters.setPdpGroup(refProp.getStringValue("policy.pdp.group"));
        deletePolicyParameters.setPolicyType(policyType);
        // send delete request
        StringBuilder responseMessage = new StringBuilder(sendDeletePolicy(deletePolicyParameters, prop));
        logger.info("Deleting policy from PAP...");
        deletePolicyParameters.setPolicyComponent("PAP");
        deletePolicyParameters.setDeleteCondition(DeletePolicyCondition.ALL);
        // send delete request
        responseMessage.append(sendDeletePolicy(deletePolicyParameters, prop));
        return responseMessage.toString();
    }

    /**
     * Send delete request to Policy.
     *
     * @param deletePolicyParameters
     *        The DeletePolicyParameters
     * @param prop
     *        The ModelProperties
     * @return The response message from policy
     */
    protected String sendDeletePolicy(DeletePolicyParameters deletePolicyParameters, ModelProperties prop) {
        // Verify whether it is triggered by Validation Test button from UI
        if (prop.isTestOnly()) {
            return "delete not executed for test action";
        }
        // API method to create or update Policy.
        PolicyChangeResponse response = null;
        String responseMessage = "";
        try {
            logger.info("Attempting to delete policy...");
            response = getPolicyEngine().deletePolicy(deletePolicyParameters);
            responseMessage = response.getResponseMessage();
        } catch (Exception e) {
            logger.error("Exception occurred during policy communnication", e);
        }
        logger.info(LOG_POLICY_PREFIX + responseMessage);
        if (response != null && response.getResponseCode() == 200) {
            logger.info("Policy delete successful");
        } else {
            logger.warn("Policy delete failed: " + responseMessage);
            throw new BadRequestException("Policy delete failed: " + responseMessage);
        }
        return responseMessage;
    }

    /**
     * Create a temp Tosca model file and perform import model to Policy Engine
     *
     * @param cldsToscaModel
     *        Policy model details
     * @return The response message from policy
     */
    public String importToscaModel(CldsToscaModel cldsToscaModel) {
        String filePath = "";
        try {
            String clampToscaPath = refProp.getStringValue(TOSCA_FILE_TEMP_PATH);
            filePath = buildFilePathForToscaFile(clampToscaPath, cldsToscaModel.getToscaModelName());
            logger.info("Writing Tosca model : " + filePath);
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            // Create or Ovewrite an existing the file
            try (OutputStream out = Files.newOutputStream(path)) {
                out.write(cldsToscaModel.getToscaModelYaml().getBytes(), 0,
                    cldsToscaModel.getToscaModelYaml().getBytes().length);
            }
        } catch (IOException e) {
            logger.error("Exception caught when attempting to write Tosca files to disk", e);
            throw new PolicyClientException("Exception caught when attempting to write Tosca files to disk", e);
        }

        ImportParameters importParameters = new ImportParameters();
        importParameters.setImportParameters(cldsToscaModel.getToscaModelName(), cldsToscaModel.getToscaModelName(),
            null, filePath, IMPORT_TYPE.MICROSERVICE, String.valueOf(cldsToscaModel.getVersion()));
        return importModel(importParameters);
    }

    /**
     * @param importParameters
     *        The ImportParameters
     * @return The response message from policy
     */
    protected String importModel(ImportParameters importParameters) {
        PolicyChangeResponse response = null;
        String responseMessage = "";

        try {
            logger.info("Attempting to import tosca policy model for action=" + importParameters.getFilePath());
            response = getPolicyEngine().policyEngineImport(importParameters);
            if (response != null) {
                responseMessage = response.getResponseMessage();
            }
        } catch (Exception e) {
            LoggingUtils.setResponseContext("900", "Policy Model import failed", this.getClass().getName());
            LoggingUtils.setErrorContext("900", "Policy Model import error");
            logger.error("Exception occurred during policy communication", e);
            throw new PolicyClientException("Exception while communicating with Policy", e);
        }
        logger.info(LOG_POLICY_PREFIX + responseMessage);
        if (response != null && (response.getResponseCode() == 200 || response.getResponseCode() == 204)) {
            LoggingUtils.setResponseContext("0", "Policy Model import success", this.getClass().getName());
            logger.info("Policy import model successful");
            metricsLogger.info("Policy import model success");
        } else {
            LoggingUtils.setResponseContext("900", "Policy import model failed", this.getClass().getName());
            logger.warn("Policy import model failed: " + responseMessage);
            metricsLogger.info("Policy import model failure");
            throw new BadRequestException("Policy import model failed: " + responseMessage);
        }
        return responseMessage;
    }

    /**
     * @param clampToscaPath
     *        Temp directory path for writing tosca files
     * @param toscaModelName
     *        Tosca Model Name
     * @return File Path on the system
     */
    private String buildFilePathForToscaFile(String clampToscaPath, String toscaModelName) {
        return clampToscaPath + "/" + toscaModelName + ".yml";
    }
}
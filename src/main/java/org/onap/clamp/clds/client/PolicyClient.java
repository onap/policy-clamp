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

package org.onap.clamp.clds.client;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.onap.clamp.clds.exception.policy.PolicyClientException;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.policy.api.AttributeType;
import org.onap.policy.api.ConfigRequestParameters;
import org.onap.policy.api.DeletePolicyCondition;
import org.onap.policy.api.DeletePolicyParameters;
import org.onap.policy.api.PolicyChangeResponse;
import org.onap.policy.api.PolicyConfig;
import org.onap.policy.api.PolicyConfigException;
import org.onap.policy.api.PolicyConfigType;
import org.onap.policy.api.PolicyEngine;
import org.onap.policy.api.PolicyEngineException;
import org.onap.policy.api.PolicyParameters;
import org.onap.policy.api.PolicyType;
import org.onap.policy.api.PushPolicyParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

/**
 * Policy utility methods - specifically, send the policy.
 */
public class PolicyClient {

    protected static final String     POLICY_PREFIX_BASE         = "Config_";
    protected static final String     POLICY_PREFIX_BRMS_PARAM   = "Config_BRMS_Param_";
    protected static final String     POLICY_PREFIX_MICROSERVICE = "Config_MS_";

    protected static final String     LOG_POLICY_PREFIX          = "Response is ";

    protected static final EELFLogger logger                     = EELFManager.getInstance()
            .getLogger(PolicyClient.class);
    protected static final EELFLogger metricsLogger              = EELFManager.getInstance().getMetricsLogger();

    @Value("${org.onap.clamp.config.files.cldsPolicyConfig:'classpath:/clds/clds-policy-config.properties'}")
    protected String                  cldsPolicyConfigFile;

    @Autowired
    protected ApplicationContext      appContext;

    @Autowired
    protected RefProp                 refProp;

    /**
     * Perform BRMS policy type.
     *
     * @param attributes
     *            A map of attributes
     * @param prop
     *            The ModelProperties
     * @param policyRequestUuid
     *            PolicyRequest UUID
     * @return The response message of policy
     * 
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
        String policyNamePrefix = refProp.getStringValue("policy.op.policyNamePrefix");
        String rtnMsg = send(policyParameters, prop, policyNamePrefix);

        String policyType = refProp.getStringValue("policy.op.type");
        push(policyType, prop);

        return rtnMsg;
    }

    /**
     * Perform send of microservice policy in JSON.
     *
     * @param policyJson
     *            The policy JSON
     * @param prop
     *            The ModelProperties
     * @param policyRequestUuid
     *            The policy Request UUID
     * @return The response message of policy
     */
    public String sendMicroServiceInJson(String policyJson, ModelProperties prop, String policyRequestUuid) {

        PolicyParameters policyParameters = new PolicyParameters();

        // Set Policy Type
        policyParameters.setPolicyConfigType(PolicyConfigType.MicroService);
        policyParameters.setEcompName(refProp.getStringValue("policy.onap.name"));
        policyParameters.setPolicyName(prop.getCurrentPolicyScopeAndPolicyName());

        policyParameters.setConfigBody(policyJson);
        policyParameters.setConfigBodyType(PolicyType.JSON);

        policyParameters.setRequestID(UUID.fromString(policyRequestUuid));
        String policyNamePrefix = refProp.getStringValue("policy.ms.policyNamePrefix");

        // Adding this line to clear the policy id from policy name while
        // pushing to policy engine
        prop.setPolicyUniqueId("");

        String rtnMsg = send(policyParameters, prop, policyNamePrefix);
        String policyType = refProp.getStringValue("policy.ms.type");
        push(policyType, prop);

        return rtnMsg;
    }

    /**
     * Perform send of base policy in OTHER type.
     *
     * @param configBody
     *            The config policy string body
     * @param configPolicyName
     *            The config policy name of the component that has been pre-deployed in DCAE
     * @param prop
     *            The ModelProperties
     * @param policyRequestUuid
     *            The policy request UUID
     * @return The answer from policy call
     */
    public String sendBasePolicyInOther(String configBody, String configPolicyName, ModelProperties prop, String policyRequestUuid) {

        PolicyParameters policyParameters = new PolicyParameters();

        // Set Policy Type
        policyParameters.setPolicyConfigType(PolicyConfigType.Base);
        policyParameters.setEcompName(refProp.getStringValue("policy.onap.name"));
        policyParameters.setPolicyName(prop.getCurrentPolicyScopeAndPolicyName());

        policyParameters.setConfigBody(configBody);
        policyParameters.setConfigBodyType(PolicyType.OTHER);
        policyParameters.setConfigName("HolmesPolicy");
        policyParameters.setPolicyName(configPolicyName);

        policyParameters.setRequestID(UUID.fromString(policyRequestUuid));

        // Adding this line to clear the policy id from policy name while
        // pushing to policy engine
        prop.setPolicyUniqueId("");

        String rtnMsg = send(policyParameters, prop, POLICY_PREFIX_BASE);
        push(PolicyConfigType.Base.toString(), prop);

        return rtnMsg;
    }

    /**
     * Perform send of Microservice policy in OTHER type.
     * 
     * @param configBody
     *            The config policy string body
     * @param prop
     *            The ModelProperties
     * @param policyRequestUuid
     *            The policy request UUID
     * @return The answer from policy call
     */
    public String sendMicroServiceInOther(String configBody, ModelProperties prop, String policyRequestUuid) {

        PolicyParameters policyParameters = new PolicyParameters();

        // Set Policy Type
        policyParameters.setPolicyConfigType(PolicyConfigType.MicroService);
        policyParameters.setEcompName(refProp.getStringValue("policy.onap.name"));
        policyParameters.setPolicyName(prop.getCurrentPolicyScopeAndPolicyName());

        policyParameters.setConfigBody(configBody);
        policyParameters.setConfigBodyType(PolicyType.OTHER);

        policyParameters.setRequestID(UUID.fromString(policyRequestUuid));
        String policyNamePrefix = refProp.getStringValue("policy.ms.policyNamePrefix");

        // Adding this line to clear the policy id from policy name while
        // pushing to policy engine
        prop.setPolicyUniqueId("");

        String rtnMsg = send(policyParameters, prop, policyNamePrefix);
        String policyType = refProp.getStringValue("policy.ms.type");
        push(policyType, prop);

        return rtnMsg;
    }

    /**
     * Perform send of policy.
     *
     * @param policyParameters
     *            The PolicyParameters
     * @param prop
     *            The ModelProperties
     * @return The response message of Policy
     */
    protected String send(PolicyParameters policyParameters, ModelProperties prop, String policyNamePrefix) {
        // Verify whether it is triggered by Validation Test button from UI
        if (prop.isTest()) {
            return "send not executed for test action";
        }

        // API method to create or update Policy.
        PolicyChangeResponse response = null;
        String responseMessage = "";
        Date startTime = new Date();
        try {
            List<Integer> versions = getVersions(policyNamePrefix, prop);
            if (versions.isEmpty()) {
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
            logger.error("Exception occurred during policy communication", e);
            throw new PolicyClientException("Exception while communicating with Policy", e);
        }
        logger.info(LOG_POLICY_PREFIX + responseMessage);

        LoggingUtils.setTimeContext(startTime, new Date());

        if (response.getResponseCode() == 200) {
            logger.info("Policy send successful");
            metricsLogger.info("Policy send success");
        } else {
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
     *            The policy Type
     * @param prop
     *            The ModelProperties
     * @return The response message of policy
     */
    protected String push(String policyType, ModelProperties prop) {
        // Verify whether it is triggered by Validation Test button from UI
        if (prop.isTest()) {
            return "push not executed for test action";
        }

        PushPolicyParameters pushPolicyParameters = new PushPolicyParameters();

        // Parameter arguments
        if (prop.getPolicyUniqueId() != null && !prop.getPolicyUniqueId().isEmpty()) {
            pushPolicyParameters.setPolicyName(prop.getPolicyScopeAndNameWithUniqueId());
        } else {
            pushPolicyParameters.setPolicyName(prop.getCurrentPolicyScopeAndPolicyName());
        }
        logger.info("Policy Name in Push policy method - " + pushPolicyParameters.getPolicyName());

        pushPolicyParameters.setPolicyType(policyType);
        pushPolicyParameters.setPdpGroup(refProp.getStringValue("policy.pdp.group"));
        pushPolicyParameters.setRequestID(null);

        // API method to create or update Policy.
        PolicyChangeResponse response = null;
        String responseMessage = "";
        try {
            logger.info("Attempting to push policy...");
            response = getPolicyEngine().pushPolicy(pushPolicyParameters);
            responseMessage = response.getResponseMessage();
        } catch (Exception e) {
            logger.error("Exception occurred during policy communication", e);
        }
        logger.info(LOG_POLICY_PREFIX + responseMessage);

        if (response != null && (response.getResponseCode() == 200 || response.getResponseCode() == 204)) {
            logger.info("Policy push successful");
        } else {
            logger.warn("Policy push failed: " + responseMessage);
            throw new BadRequestException("Policy push failed: " + responseMessage);
        }

        return responseMessage;
    }

    /**
     * Use Get Config Policy API to retrieve the versions for a policy. Return
     * versions in sorted order. Return empty list if none found.
     *
     * @param policyNamePrefix
     *            The Policy Name Prefix
     * @param prop
     *            The ModelProperties
     * @return The response message from policy
     * @throws PolicyConfigException
     *             In case of issues with policy engine
     */
    protected List<Integer> getVersions(String policyNamePrefix, ModelProperties prop) throws PolicyConfigException {

        ArrayList<Integer> versions = new ArrayList<>();
        ConfigRequestParameters configRequestParameters = new ConfigRequestParameters();
        String policyName = "";

        if (prop.getPolicyUniqueId() != null && !prop.getPolicyUniqueId().isEmpty()) {
            policyName = prop.getCurrentPolicyScopeAndFullPolicyName(policyNamePrefix) + "_" + prop.getPolicyUniqueId();
        } else {
            policyName = prop.getCurrentPolicyScopeAndFullPolicyName(policyNamePrefix);
        }

        logger.info("policyName=" + policyName);
        configRequestParameters.setPolicyName(policyName);

        Collection<PolicyConfig> response = getPolicyEngine().getConfig(configRequestParameters);
        for (PolicyConfig policyConfig : response) {
            Integer version = Integer.valueOf(policyConfig.getPolicyVersion());
            versions.add(version);
        }
        Collections.sort(versions);
        logger.info("Policy versions.size()=" + versions.size());

        return versions;

    }

    /**
     * This method create a new policy engine.
     * 
     * @return A new policy engine
     */
    private PolicyEngine getPolicyEngine() {
        PolicyEngine policyEngine;
        try {
            policyEngine = new PolicyEngine(appContext.getResource(cldsPolicyConfigFile).getFile().getAbsolutePath());
        } catch (IOException e1) {
            throw new PolicyClientException("Exception when opening policy config file", e1);
        } catch (PolicyEngineException e) {
            throw new PolicyClientException("Exception when creating a new policy engine", e);
        }
        return policyEngine;
    }

    /**
     * Format and send delete Micro Service requests to Policy.
     *
     * @param prop
     *            The ModelProperties
     * @return The response message from Policy
     */
    public String deleteMicrosService(ModelProperties prop) {
        String policyType = refProp.getStringValue("policy.ms.type");
        return deletePolicy(prop, policyType);
    }

    /**
     * This method delete the Base policy.
     *
     * @param prop
     *            The model Properties
     * @return A string with the answer from policy
     */
    public String deleteBasePolicy(ModelProperties prop) {
        return deletePolicy(prop, PolicyConfigType.Base.toString());
    }

    /**
     * Format and send delete BRMS requests to Policy.
     *
     * @param prop
     *            The ModelProperties
     * @return The response message from policy
     */
    public String deleteBrms(ModelProperties prop) {
        String policyType = refProp.getStringValue("policy.op.type");
        return deletePolicy(prop, policyType);
    }

    /**
     * Format and send delete PAP and PDP requests to Policy.
     *
     * @param prop
     *            The ModelProperties
     *
     * @return The response message from policy
     */
    protected String deletePolicy(ModelProperties prop, String policyType) {
        DeletePolicyParameters deletePolicyParameters = new DeletePolicyParameters();

        if (prop.getPolicyUniqueId() != null && !prop.getPolicyUniqueId().isEmpty()) {
            deletePolicyParameters.setPolicyName(prop.getPolicyScopeAndNameWithUniqueId());
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
     *            The DeletePolicyParameters
     * @param prop
     *            The ModelProperties
     * @return The response message from policy
     */
    protected String sendDeletePolicy(DeletePolicyParameters deletePolicyParameters, ModelProperties prop) {
        // Verify whether it is triggered by Validation Test button from UI
        if (prop.isTest()) {
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
}
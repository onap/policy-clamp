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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.onap.policy.api.AttributeType;
import org.onap.policy.api.ConfigRequestParameters;
import org.onap.policy.api.DeletePolicyCondition;
import org.onap.policy.api.DeletePolicyParameters;
import org.onap.policy.api.PolicyChangeResponse;
import org.onap.policy.api.PolicyConfig;
import org.onap.policy.api.PolicyConfigType;
import org.onap.policy.api.PolicyEngine;
import org.onap.policy.api.PolicyParameters;
import org.onap.policy.api.PolicyType;
import org.onap.policy.api.PushPolicyParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 * Policy utility methods - specifically, send the policy.
 */
public class PolicyClient {
    protected static final EELFLogger logger        = EELFManager.getInstance().getLogger(PolicyClient.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    @Value("${org.onap.clamp.config.files.cldsPolicyConfig:'classpath:/clds/clds-policy-config.properties'}")
    protected String                cldsPolicyConfigFile;

    @Autowired
    protected ApplicationContext    appContext;

    @Autowired
    protected RefProp               refProp;

    public PolicyClient() {

    }

    /**
     * Perform send of microservice policy
     *
     * @param attributes
     * @param prop
     * @param policyRequestUUID
     * @return
     * @throws Exception
     */
    public String sendBrms(Map<AttributeType, Map<String, String>> attributes, ModelProperties prop,
            String policyRequestUUID) throws Exception {

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
        policyParameters.setRequestID(UUID.fromString(policyRequestUUID));
        String policyNamePrefix = refProp.getStringValue("policy.op.policyNamePrefix");
		String rtnMsg = send(policyParameters, prop, policyNamePrefix);

        String policyType = refProp.getStringValue("policy.op.type");
        push(policyType, prop);

        return rtnMsg;
    }

    /**
     * Perform send of microservice policy
     *
     * @param policyJson
     * @param prop
     * @param policyRequestUUID
     * @return
     * @throws Exception
     */
    public String sendMicroService(String policyJson, ModelProperties prop, String policyRequestUUID) throws Exception {

        PolicyParameters policyParameters = new PolicyParameters();

        // Set Policy Type
        policyParameters.setPolicyConfigType(PolicyConfigType.MicroService);
        policyParameters.setOnapName(refProp.getStringValue("policy.ecomp.name"));
        policyParameters.setPolicyName(prop.getCurrentPolicyScopeAndPolicyName());

        policyParameters.setConfigBody(policyJson);
        policyParameters.setConfigBodyType(PolicyType.JSON);

        policyParameters.setRequestID(UUID.fromString(policyRequestUUID));
        String policyNamePrefix = refProp.getStringValue("policy.ms.policyNamePrefix");
		prop.setPolicyUniqueId("");//Adding this line to clear the policy id from policy name while pushing to policy engine
		String rtnMsg = send(policyParameters, prop, policyNamePrefix);
		String policyType = refProp.getStringValue("policy.ms.type");
		push(policyType, prop);

        return rtnMsg;
    }

    /**
     * Perform send of policy.
     *
     * @param policyParameters
     * @param prop
     * @return
     * @throws Exception
     */
    protected String send(PolicyParameters policyParameters, ModelProperties prop, String policyNamePrefix) throws Exception {
    	// Verify whether it is triggered by Validation Test button from UI
		if ( prop.isTest() ) {
			return "send not executed for test action";
		}

        PolicyEngine policyEngine = new PolicyEngine(
                appContext.getResource(cldsPolicyConfigFile).getFile().getAbsolutePath()); 

        // API method to create or update Policy.
        PolicyChangeResponse response = null;
        String responseMessage;
        try {
        	List<Integer> versions = getVersions(policyNamePrefix, prop);
			if (versions.size() <= 0) {
                logger.info("Attempting to create policy for action=" + prop.getActionCd());
                response = policyEngine.createPolicy(policyParameters);
                responseMessage = response.getResponseMessage();
            } else {
                logger.info("Attempting to update policy for action=" + prop.getActionCd());
                response = policyEngine.updatePolicy(policyParameters);
                responseMessage = response.getResponseMessage();
            }
        } catch (Exception e) {
            responseMessage = e.toString();
        }
        logger.info("response is " + responseMessage);

        if (response != null && response.getResponseCode() == 200) {
            logger.info("Policy send successful");
        } else {
            logger.warn("Policy send failed: " + responseMessage);
            throw new Exception("Policy send failed: " + responseMessage);
        }

        return responseMessage;
    }

    /**
     * Format and send push of policy.
     *
     * @param policyType
     * @param prop
     * @return
     * @throws Exception
     */
    protected String push(String policyType, ModelProperties prop) throws Exception {
    	// Verify whether it is triggered by Validation Test button from UI
    	if ( prop.isTest() ) {
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

        PolicyEngine policyEngine = new PolicyEngine(
                appContext.getResource(cldsPolicyConfigFile).getFile().getAbsolutePath());

        // API method to create or update Policy.
        PolicyChangeResponse response = null;
        String responseMessage;
        try {
            logger.info("Attempting to push policy...");
            response = policyEngine.pushPolicy(pushPolicyParameters);
            responseMessage = response.getResponseMessage();
        } catch (Exception e) {
            responseMessage = e.toString();
        }
        logger.info("response is " + responseMessage);

        if (response != null && (response.getResponseCode() == 200 || response.getResponseCode() == 204)) {
            logger.info("Policy push successful");
        } else {
            logger.warn("Policy push failed: " + responseMessage);
            throw new Exception("Policy push failed: " + responseMessage);
        }

        return responseMessage;
    }

    /**
     * Use Get Config Policy API to retrieve the versions for a policy. Return
     * versions in sorted order. Return empty list if none found.
     *
     * @param policyNamePrefix
     * @param prop
     * @return
     * @throws Exception
     */
    protected List<Integer> getVersions(String policyNamePrefix, ModelProperties prop) throws Exception {

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

        PolicyEngine policyEngine = new PolicyEngine(
                appContext.getResource(cldsPolicyConfigFile).getFile().getAbsolutePath());

		try {
			Collection<PolicyConfig> response = policyEngine.getConfig(configRequestParameters);
			Iterator<PolicyConfig> itrResp = response.iterator();

			while (itrResp.hasNext()) {
				PolicyConfig policyConfig = itrResp.next();
            	try {
            		Integer version = new Integer(policyConfig.getPolicyVersion());
            		versions.add(version);
            	} catch (Exception e) {
            		// just print warning - if n;o policies, version may be null
            		logger.warn(
                        "warning: failed to parse policyConfig.getPolicyVersion()=" + policyConfig.getPolicyVersion());
            	}
			}
			Collections.sort(versions);
			logger.info("Policy versions.size()=" + versions.size());	
		} catch (Exception e) {
			// just print warning - if no policy version found
			logger.warn("warning: policy not found...policy name - " + policyName);
		}

        return versions;
    }

    /**
     * Format and send delete Micro Service requests to Policy
     *
     * @param prop
     * @return
     * @throws Exception
     */
    public String deleteMicrosService(ModelProperties prop) throws Exception {
        String policyNamePrefix = refProp.getStringValue("policy.ms.policyNamePrefix");
        String policyType = refProp.getStringValue("policy.ms.type");
        return deletePolicy(policyNamePrefix, prop, policyType);
    }

    /**
     * Format and send delete BRMS requests to Policy
     *
     * @param prop
     * @return
     * @throws Exception
     */
    public String deleteBrms(ModelProperties prop) throws Exception {
        String policyNamePrefix = refProp.getStringValue("policy.op.policyNamePrefix");
        String policyType = refProp.getStringValue("policy.op.type");
        return deletePolicy(policyNamePrefix, prop, policyType);
    }

    /**
     * Format and send delete PAP and PDP requests to Policy
     *
     * @param policyNamePrefix
     * @param prop
     * @return
     * @throws Exception
     */
    protected String deletePolicy(String policyNamePrefix, ModelProperties prop, String policyType) throws Exception {
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
        String responseMessage = null;
        responseMessage = sendDeletePolicy(deletePolicyParameters, prop);

        logger.info("Deleting policy from PAP...");
        deletePolicyParameters.setPolicyComponent("PAP");
        deletePolicyParameters.setDeleteCondition(DeletePolicyCondition.ALL);

        // send delete request
        responseMessage = sendDeletePolicy(deletePolicyParameters, prop);

        return responseMessage;
    }

    /**
     * Send delete request to Policy
     *
     * @param deletePolicyParameters
     * @param prop
     * @return
     * @throws Exception
     */
    protected String sendDeletePolicy(DeletePolicyParameters deletePolicyParameters, ModelProperties prop)
            throws Exception {
    	// Verify whether it is triggered by Validation Test button from UI
		if ( prop.isTest() ) {
			return "delete not executed for test action";
		}		
        PolicyEngine policyEngine = new PolicyEngine(
                appContext.getResource(cldsPolicyConfigFile).getFile().getAbsolutePath());

        // API method to create or update Policy.
        PolicyChangeResponse response = null;
        String responseMessage;
        try {
            logger.info("Attempting to delete policy...");
            response = policyEngine.deletePolicy(deletePolicyParameters);
            responseMessage = response.getResponseMessage();
        } catch (Exception e) {
            responseMessage = e.toString();
        }
        logger.info("response is " + responseMessage);

        if (response != null && response.getResponseCode() == 200) {
            logger.info("Policy delete successful");
        } else {
            logger.warn("Policy delete failed: " + responseMessage);
            throw new Exception("Policy delete failed: " + responseMessage);
        }

        return responseMessage;
    }
}
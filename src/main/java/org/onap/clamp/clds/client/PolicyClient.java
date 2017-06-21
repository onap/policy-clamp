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

import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.openecomp.policy.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.logging.Logger;


/**
 * Policy utility methods - specifically, send the policy.
 */
public class PolicyClient {
    // currently uses the java.util.logging.Logger like the Camunda engine
    private static final Logger logger = Logger.getLogger(PolicyClient.class.getName());

    @Value("${org.onap.clamp.config.files.cldsPolicyConfig:'classpath:etc/clds/clds-policy-config.properties'}")
    private String cldsPolicyConfigFile;

    @Autowired
    private ApplicationContext appContext;
    
    @Autowired
    private RefProp refProp;
    
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
    public String sendBrms(Map<AttributeType, Map<String, String>> attributes, ModelProperties prop, String policyRequestUUID) throws Exception {

        PolicyParameters policyParameters = new PolicyParameters();

        // Set Policy Type(Mandatory)
        policyParameters.setPolicyConfigType(PolicyConfigType.BRMS_PARAM);

        // Set Policy Name(Mandatory)
        policyParameters.setPolicyName(prop.getCurrentPolicyScopeAndPolicyName());
        //Set Scope folder where the policy needs to be created(Mandatory)
        //policyParameters.setPolicyScope(policyScope);

        // documentation says this is options, but when tested, got the following failure: java.lang.Exception: Policy send failed: PE300 - Data Issue: No policyDescription given.
        policyParameters.setPolicyDescription(refProp.getStringValue("op.policyDescription"));

        policyParameters.setAttributes(attributes);

        //Set a random UUID(Mandatory)
        policyParameters.setRequestID(UUID.fromString(policyRequestUUID));

        String rtnMsg = send(policyParameters, prop);

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
        policyParameters.setEcompName(refProp.getStringValue("policy.ecomp.name"));
        policyParameters.setPolicyName(prop.getCurrentPolicyScopeAndPolicyName());

        policyParameters.setConfigBody(policyJson);
        policyParameters.setConfigBodyType(PolicyType.JSON);

        policyParameters.setRequestID(UUID.fromString(policyRequestUUID));

        String rtnMsg = send(policyParameters, prop);

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
    private String send(PolicyParameters policyParameters, ModelProperties prop) throws Exception {
        PolicyEngine policyEngine = new PolicyEngine(appContext.getResource(cldsPolicyConfigFile).getFile().getAbsolutePath());

        // API method to create or update Policy.
        PolicyChangeResponse response = null;
        String responseMessage;
        try {
            if (prop.isCreateRequest()) {
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
            logger.warning("Policy send failed: " + responseMessage);
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
    private String push(String policyType, ModelProperties prop) throws Exception {
        PushPolicyParameters pushPolicyParameters = new PushPolicyParameters();

        //Parameter arguments
        pushPolicyParameters.setPolicyName(prop.getCurrentPolicyScopeAndPolicyName());
        pushPolicyParameters.setPolicyType(policyType);
        //pushPolicyParameters.setPolicyScope(policyScope);
        pushPolicyParameters.setPdpGroup(refProp.getStringValue("policy.pdp.group"));
        pushPolicyParameters.setRequestID(null);

        PolicyEngine policyEngine = new PolicyEngine(appContext.getResource(cldsPolicyConfigFile).getFile().getAbsolutePath());

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
            logger.warning("Policy push failed: " + responseMessage);
            throw new Exception("Policy push failed: " + responseMessage);
        }

        return responseMessage;
    }

    /**
     * Use Get Config Policy API to retrieve the versions for a policy.
     * Return versions in sorted order.
     * Return empty list if none found.
     *
     * @param policyNamePrefix
     * @param prop
     * @return
     * @throws Exception
     */
    private List<Integer> getVersions(String policyNamePrefix, ModelProperties prop) throws Exception {

        ArrayList<Integer> versions = new ArrayList<>();
        ConfigRequestParameters configRequestParameters = new ConfigRequestParameters();
        String policyName = prop.getCurrentPolicyScopeAndFullPolicyName(policyNamePrefix);
        logger.info("policyName=" + policyName);
        configRequestParameters.setPolicyName(policyName);

        PolicyEngine policyEngine = new PolicyEngine(appContext.getResource(cldsPolicyConfigFile).getFile().getAbsolutePath());

        Collection<PolicyConfig> response = policyEngine.getConfig(configRequestParameters);

        Iterator<PolicyConfig> itrResp = response.iterator();

        while (itrResp.hasNext()) {
            PolicyConfig policyConfig = itrResp.next();
            try {
                Integer version = new Integer(policyConfig.getPolicyVersion());
                versions.add(version);
            } catch (Exception e) {
                // just print warning - if n;o policies, version may be null
                logger.warning("warning: failed to parse policyConfig.getPolicyVersion()=" + policyConfig.getPolicyVersion());
            }
        }
        Collections.sort(versions);
        logger.info("versions.size()=" + versions.size());

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
        return deletePolicy(policyNamePrefix, prop);
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
        return deletePolicy(policyNamePrefix, prop);
    }

    /**
     * Format and send delete PAP and PDP requests to Policy
     *
     * @param policyNamePrefix
     * @param prop
     * @return
     * @throws Exception
     */
    private String deletePolicy(String policyNamePrefix, ModelProperties prop) throws Exception {
        String responseMessage = null;

        DeletePolicyParameters deletePolicyParameters = new DeletePolicyParameters();

        List<Integer> versions = getVersions(policyNamePrefix, prop);
        if (versions.size() > 0) {
            int maxVersion = Collections.max(versions);

            // format delete all PAP request
            deletePolicyParameters.setPolicyName(prop.getCurrentPolicyScopeAndFullPolicyNameWithVersion(policyNamePrefix, maxVersion));
            deletePolicyParameters.setPolicyComponent("PAP");
            deletePolicyParameters.setDeleteCondition(DeletePolicyCondition.ALL);
            String policyType = refProp.getStringValue("policy.ms.type");
            deletePolicyParameters.setPolicyType(policyType);

            //send delete request
            responseMessage = sendDeletePolicy(deletePolicyParameters, prop);
        }

        for (Integer version : versions) {
            // format delete all PDP request
            deletePolicyParameters.setPolicyName(prop.getCurrentPolicyScopeAndFullPolicyNameWithVersion(policyNamePrefix, version));
            deletePolicyParameters.setPolicyComponent("PDP");
            deletePolicyParameters.setPdpGroup(refProp.getStringValue("policy.pdp.group"));
            //send delete request
            responseMessage = responseMessage + "; " + sendDeletePolicy(deletePolicyParameters, prop);
        }

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
    private String sendDeletePolicy(DeletePolicyParameters deletePolicyParameters, ModelProperties prop) throws Exception {
        PolicyEngine policyEngine = new PolicyEngine(appContext.getResource(cldsPolicyConfigFile).getFile().getAbsolutePath());

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
            logger.warning("Policy delete failed: " + responseMessage);
            throw new Exception("Policy delete failed: " + responseMessage);
        }

        return responseMessage;
    }
}
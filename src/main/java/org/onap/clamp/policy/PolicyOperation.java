/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

package org.onap.clamp.policy;

import java.io.IOException;
import java.util.Set;

import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.util.HttpConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Component
public class PolicyOperation {
    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(PolicyOperation.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    public static final String POLICY_MSTYPE_PROPERTY_NAME = "policy.ms.type";
    public static final String POLICY_ONAPNAME_PROPERTY_NAME = "policy.onap.name";
    public static final String POLICY_BASENAME_PREFIX_PROPERTY_NAME = "policy.base.policyNamePrefix";
    public static final String POLICY_OP_NAME_PREFIX_PROPERTY_NAME = "policy.op.policyNamePrefix";
    public static final String POLICY_MS_NAME_PREFIX_PROPERTY_NAME = "policy.ms.policyNamePrefix";
    public static final String POLICY_OP_TYPE_PROPERTY_NAME = "policy.op.type";
    public static final String POLICY_GUARD_SUFFIX = "_Guard";
    public static final String POLICY_URL_PROPERTY_NAME = "clamp.config.policy.url";
    public static final String POLICY_URL_SUFFIX = "/versions/1.0.0/policies";
    public static final String POLICY_USER_NAME = "clamp.config.policy.userName";
    public static final String POLICY_PASSWORD = "clamp.config.policy.password";

    public static final String TOSCA_DEF_VERSION = "tosca_definitions_version";
    public static final String TOSCA_DEF_VERSION_VALUE = "tosca_simple_yaml_1_0_0";
    public static final String TEMPLATE = "topology_template";
    public static final String POLICIES = "policies";
    public static final String MS_TYPE = "type";
    public static final String MS_VERSION = "version";
    public static final String MS_VERSION_VALUE = "1.0.0";
    public static final String MS_METADATA = "metadata";
    public static final String MS_POLICY_ID = "policy_id";
    public static final String MS_PROPERTIES = "properties";
    public static final String MS_policy = "tca_policy";

    private final ClampProperties refProp;
    private final HttpConnectionManager httpConnectionManager;

    @Autowired
    public PolicyOperation(ClampProperties refProp, HttpConnectionManager httpConnectionManager) {
        this.refProp = refProp;
        this.httpConnectionManager = httpConnectionManager;
    }

    public void createMsPolicy(Set<MicroServicePolicy> policyList) throws IOException {
        // Get policy first? if exist delete???
        // push pdp group
        for (MicroServicePolicy msPolicy:policyList) {
            JsonObject payload = createMsPolicyPayload(msPolicy);
            String policyType = msPolicy.getModelType();
            String url = refProp.getStringValue(POLICY_URL_PROPERTY_NAME) + policyType + POLICY_URL_SUFFIX;
            String userName = refProp.getStringValue(POLICY_USER_NAME);
            String encodedPass = refProp.getStringValue(POLICY_PASSWORD);
            httpConnectionManager.doHttpRequest(url, "POST", payload.toString(), "application/json", "POLICY", userName, encodedPass);
        }
    }

    public void deleteMsPolicy(Set<MicroServicePolicy> policyList) throws IOException {
        for (MicroServicePolicy msPolicy:policyList) {
            String policyType = msPolicy.getModelType();
            String url = refProp.getStringValue(POLICY_URL_PROPERTY_NAME) + policyType + POLICY_URL_SUFFIX + "/" + msPolicy.getName();
            String userName = refProp.getStringValue(POLICY_USER_NAME);
            String encodedPass = refProp.getStringValue(POLICY_PASSWORD);
            httpConnectionManager.doHttpRequest(url, "POST", null, null, "POLICY", userName, encodedPass);
        }
    }

    private JsonObject createMsPolicyPayload(MicroServicePolicy microService) {
        JsonObject policyConfig = new JsonObject();
        policyConfig.add(MS_policy, microService.getProperties());

        JsonObject properties = new JsonObject();
        properties.add(MS_policy, policyConfig);

        JsonObject msPolicy = new JsonObject();
        msPolicy.addProperty(MS_TYPE, microService.getModelType());
        msPolicy.addProperty(MS_VERSION, MS_VERSION_VALUE);
        JsonObject metaData = new JsonObject();
        metaData.addProperty(MS_POLICY_ID, microService.getName());
        msPolicy.add(MS_METADATA, metaData);
        msPolicy.add(MS_PROPERTIES, properties);

        JsonObject msPolicyWithName = new JsonObject();
        msPolicyWithName.add(microService.getName(), msPolicy);

        JsonArray policyArray = new JsonArray();
        policyArray.add(msPolicyWithName);

        JsonObject template =  new JsonObject();
        template.add(POLICIES, policyArray);

        JsonObject configPolicy = new JsonObject();
        configPolicy.addProperty(TOSCA_DEF_VERSION, TOSCA_DEF_VERSION_VALUE);
        configPolicy.add(TEMPLATE, template);

        return configPolicy;
    }

}

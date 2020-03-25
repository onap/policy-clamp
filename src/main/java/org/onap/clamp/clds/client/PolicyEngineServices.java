/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.client;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.sdc.controller.installer.BlueprintMicroService;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.loop.template.PolicyModel;
import org.onap.clamp.loop.template.PolicyModelId;
import org.onap.clamp.loop.template.PolicyModelsService;
import org.onap.clamp.policy.pdpgroup.PdpGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;


/**
 * The class implements the communication with the Policy Engine to retrieve
 * policy models (tosca). It mainly delegates the physical calls to Camel
 * engine.
 */
@Component
public class PolicyEngineServices {
    private final CamelContext camelContext;

    private final PolicyModelsService policyModelsService;

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(PolicyEngineServices.class);
    private static int retryInterval = 0;
    private static int retryLimit = 1;

    public static final String POLICY_RETRY_INTERVAL = "policy.retry.interval";
    public static final String POLICY_RETRY_LIMIT = "policy.retry.limit";

    /**
     * Default constructor.
     *
     * @param camelContext        Camel context bean
     * @param clampProperties     ClampProperties bean
     * @param policyModelsService policyModel service
     */
    @Autowired
    public PolicyEngineServices(CamelContext camelContext, ClampProperties clampProperties,
                                PolicyModelsService policyModelsService) {
        this.camelContext = camelContext;
        this.policyModelsService = policyModelsService;
        if (clampProperties.getStringValue(POLICY_RETRY_LIMIT) != null) {
            retryLimit = Integer.parseInt(clampProperties.getStringValue(POLICY_RETRY_LIMIT));
        }
        if (clampProperties.getStringValue(POLICY_RETRY_INTERVAL) != null) {
            retryInterval = Integer.parseInt(clampProperties.getStringValue(POLICY_RETRY_INTERVAL));
        }
    }

    /**
     * This method query Policy engine and create a PolicyModel object with type and version.
     *
     * @param policyType    The policyType id
     * @param policyVersion The policy version of that type
     * @return A PolicyModel created from policyEngine data
     */
    public PolicyModel createPolicyModelFromPolicyEngine(String policyType, String policyVersion) {
        if (!policyModelsService.existsById(
                new PolicyModelId(policyType, policyVersion))) {
            return policyModelsService.savePolicyModelInNewTransaction(
                    new PolicyModel(policyType, this.downloadOnePolicy(policyType, policyVersion), policyVersion));
        }
        logger.info("Skipping policy model download as it exists already in the database " + policyType
                + "/" + policyVersion);
        return null;
    }

    /**
     * This method query Policy engine and create a PolicyModel object with type and version.
     *
     * @param microService microservice object instance
     * @return A PolicyModel created from policyEngine data
     */
    public PolicyModel createPolicyModelFromPolicyEngine(BlueprintMicroService microService) {
        return createPolicyModelFromPolicyEngine(microService.getModelType(), microService.getModelVersion());
    }

    /**
     * This method synchronize the clamp database and the policy engine.
     * So it creates the required PolicyModel.
     */
    public void synchronizeAllPolicies() {
        LinkedHashMap<String, Object> loadedYaml;
        loadedYaml = new Yaml().load(downloadAllPolicies());
        if (loadedYaml == null || loadedYaml.isEmpty()) {
            logger.warn("getAllPolicyType yaml returned by policy engine could not be decoded, as it's null or empty");
            return;
        }

        LinkedHashMap<String, Object> policyTypesMap = (LinkedHashMap<String, Object>) loadedYaml
                .get("policy_types");
        policyTypesMap.forEach((key, value) ->
                this.createPolicyModelFromPolicyEngine(key,
                        ((String) ((LinkedHashMap<String, Object>) value).get("version"))));
    }

    /**
     * This method can be used to download all policy types + data types defined in
     * policy engine.
     *
     * @return A yaml containing all policy Types and all data types
     */
    public String downloadAllPolicies() {
        return callCamelRoute(ExchangeBuilder.anExchange(camelContext).build(), "direct:get-all-policy-models",
                "Get all policies");
    }

    /**
     * This method can be used to download a policy tosca model on the engine.
     *
     * @param policyType    The policy type (id)
     * @param policyVersion The policy version
     * @return A string with the whole policy tosca model
     */
    public String downloadOnePolicy(String policyType, String policyVersion) {
        logger.info("Downloading the policy model " + policyType + "/" + policyVersion);
        DumperOptions options = new DumperOptions();
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setIndent(4);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yamlParser = new Yaml(options);
        return yamlParser.dump((Map<String, Object>) yamlParser.load(callCamelRoute(
                ExchangeBuilder.anExchange(camelContext).withProperty("policyModelName", policyType)
                        .withProperty("policyModelVersion", policyVersion).build(), "direct:get-policy-model",
                "Get one policy")));
    }

    /**
     * This method can be used to download all Pdp Groups data from policy engine.
     */
    public void downloadPdpGroups() {
        String responseBody =
                callCamelRoute(ExchangeBuilder.anExchange(camelContext).build(), "direct:get-all-pdp-groups",
                        "Get Pdp Groups");

        if (responseBody == null || responseBody.isEmpty()) {
            logger.warn("getPdpGroups returned by policy engine could not be decoded, as it's null or empty");
            return;
        }

        JsonObject jsonObj = JsonUtils.GSON.fromJson(responseBody, JsonObject.class);

        List<PdpGroup> pdpGroupList = new LinkedList<>();
        JsonArray itemsArray = (JsonArray) jsonObj.get("groups");

        for (com.google.gson.JsonElement jsonElement : itemsArray) {
            JsonObject item = (JsonObject) jsonElement;
            PdpGroup pdpGroup = JsonUtils.GSON.fromJson(item.toString(), PdpGroup.class);
            pdpGroupList.add(pdpGroup);
        }

        policyModelsService.updatePdpGroupInfo(pdpGroupList);
    }

    private String callCamelRoute(Exchange exchange, String camelFlow, String logMsg) {
        for (int i = 0; i < retryLimit; i++) {
            Exchange exchangeResponse = camelContext.createProducerTemplate().send(camelFlow, exchange);
            if (Integer.valueOf(200).equals(exchangeResponse.getIn().getHeader("CamelHttpResponseCode"))) {
                return (String) exchangeResponse.getIn().getBody();
            }
            else {
                logger.info(logMsg + " query " + retryInterval + "ms before retrying ...");
                // wait for a while and try to connect to DCAE again
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return "";
    }
}

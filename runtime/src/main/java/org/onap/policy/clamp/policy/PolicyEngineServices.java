/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.policy;

import java.io.IOException;
import java.util.LinkedHashMap;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.onap.policy.clamp.clds.config.ClampProperties;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.clamp.loop.template.PolicyModelsService;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;


/**
 * The class implements the communication with the Policy Engine to retrieve
 * policy models (tosca). It mainly delegates the physical calls to Camel
 * engine.
 * It supports a retry mechanism for these calls, configurations can be specified in the
 * application.properties "policy.retry.interval"(default 0) and "policy.retry.limit"(default 1).
 */
@Component
public class PolicyEngineServices {
    private final CamelContext camelContext;

    private final PolicyModelsService policyModelsService;

    private static final String RAISE_EXCEPTION_FLAG = "raiseHttpExceptionFlag";

    private static final Logger logger = LoggerFactory.getLogger(PolicyEngineServices.class);
    private int retryInterval = 0;
    private int retryLimit = 1;

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
     * If the policy already exist in the db it returns the existing one.
     *
     * @param policyType    The policyType id
     * @param policyVersion The policy version of that type
     * @return A PolicyModel created from policyEngine data or null if nothing is found on policyEngine
     */
    public PolicyModel createPolicyModelFromPolicyEngine(String policyType, String policyVersion) {
        var policyModelFound = policyModelsService.getPolicyModel(policyType, policyVersion);
        if (policyModelFound == null) {
            String policyTosca = this.downloadOnePolicyToscaModel(policyType, policyVersion);
            if (policyTosca != null && !policyTosca.isEmpty()) {
                return policyModelsService.savePolicyModelInNewTransaction(
                        new PolicyModel(policyType, policyTosca, policyVersion));
            } else {
                logger.error("Policy not found in the Policy Engine, returning null: {} / {}",
                    policyType, policyVersion);
                return null;
            }
        } else {
            logger.info("Skipping policy model download as it exists already in the database {} / {}",
                policyType, policyVersion);
            return policyModelFound;
        }
    }

    /**
     * This method synchronize the clamp database and the policy engine.
     * So it creates the required PolicyModel.
     */
    @SuppressWarnings("unchecked")
    public void synchronizeAllPolicies() {
        LinkedHashMap<String, Object> loadedYaml;
        loadedYaml = new Yaml().load(downloadAllPolicyModels());
        if (loadedYaml == null || loadedYaml.isEmpty()) {
            logger.warn("getAllPolicyType yaml returned by policy engine could not be decoded, as it's null or empty");
            return;
        }

        LinkedHashMap<String, Object> policyTypesMap = (LinkedHashMap<String, Object>) loadedYaml.get("policy_types");
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
    public String downloadAllPolicyModels() {
        return callCamelRoute(
                ExchangeBuilder.anExchange(camelContext).withProperty(RAISE_EXCEPTION_FLAG, true).build(),
                "direct:get-all-policy-models", "Get all policies models");
    }

    /**
     * This method can be used to download a policy tosca model on the engine.
     *
     * @param policyType    The policy type (id)
     * @param policyVersion The policy version
     * @return A string with the whole policy tosca model
     */
    public String downloadOnePolicyToscaModel(String policyType, String policyVersion) {
        logger.info("Downloading the policy tosca model {} / {}",
             policyType, policyVersion);
        var options = new DumperOptions();
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setIndent(4);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        var yamlParser = new Yaml(options);
        String responseBody = callCamelRoute(
                ExchangeBuilder.anExchange(camelContext).withProperty("policyModelType", policyType)
                        .withProperty("policyModelVersion", policyVersion).withProperty(RAISE_EXCEPTION_FLAG, false)
                        .build(), "direct:get-policy-tosca-model",
                "Get one policy");

        if (responseBody == null || responseBody.isEmpty()) {
            logger.warn("getPolicyToscaModel returned by policy engine could not be decoded, as it's null or empty");
            return null;
        }

        return yamlParser.dump(yamlParser.load(responseBody));
    }

    /**
     * This method can be used to download all Pdp Groups data from policy engine.
     */
    public void downloadPdpGroups() {
        String responseBody =
                callCamelRoute(
                        ExchangeBuilder.anExchange(camelContext).withProperty(RAISE_EXCEPTION_FLAG, false).build(),
                        "direct:get-all-pdp-groups", "Get Pdp Groups");

        if (responseBody == null || responseBody.isEmpty()) {
            logger.warn("getPdpGroups returned by policy engine could not be decoded, as it's null or empty");
            return;
        }

        policyModelsService.updatePdpGroupInfo(JsonUtils.GSON.fromJson(responseBody, PdpGroups.class));
    }

    private String callCamelRoute(Exchange exchange, String camelFlow, String logMsg) {
        for (var i = 0; i < retryLimit; i++) {
            try (var producerTemplate = camelContext.createProducerTemplate()) {
                var exchangeResponse = producerTemplate.send(camelFlow, exchange);
                if (HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
                        .is2xxSuccessful()) {
                    return (String) exchangeResponse.getIn().getBody();
                } else {
                    logger.info("{} query ms before retrying {} ...", logMsg, retryInterval);
                    // wait for a while and try to connect to DCAE again
                    Thread.sleep(retryInterval);

                }
            } catch (IOException e) {
                logger.error("IOException caught when trying to call Camel flow: {}", camelFlow, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return "";
    }
}

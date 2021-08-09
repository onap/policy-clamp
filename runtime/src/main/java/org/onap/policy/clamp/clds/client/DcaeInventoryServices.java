/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018, 2021 AT&T Intellectual Property. All rights
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
 * Modifications copyright (c) 2018 Nokia
 * ===================================================================
 *
 */

package org.onap.policy.clamp.clds.client;

import java.io.IOException;
import java.util.Date;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.onap.policy.clamp.clds.config.ClampProperties;
import org.onap.policy.clamp.clds.model.dcae.DcaeInventoryResponse;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.clamp.clds.util.LoggingUtils;
import org.onap.policy.common.utils.logging.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * This class implements the communication with DCAE for the service inventory.
 */
@Component
public class DcaeInventoryServices {

    @Autowired
    CamelContext camelContext;

    protected static final Logger logger = LoggerFactory.getLogger(DcaeInventoryServices.class);

    public static final String DCAE_INVENTORY_URL = "dcae.inventory.url";
    public static final String DCAE_INVENTORY_RETRY_INTERVAL = "dcae.intentory.retry.interval";
    public static final String DCAE_INVENTORY_RETRY_LIMIT = "dcae.intentory.retry.limit";
    private final ClampProperties refProp;

    /**
     * Constructor.
     */
    @Autowired
    public DcaeInventoryServices(ClampProperties refProp) {
        this.refProp = refProp;
    }

    private int getTotalCountFromDcaeInventoryResponse(String responseStr) throws ParseException {
        JSONParser parser = new JSONParser();
        Object obj0 = parser.parse(responseStr);
        JSONObject jsonObj = (JSONObject) obj0;
        Long totalCount = (Long) jsonObj.get("totalCount");
        return totalCount.intValue();
    }

    private DcaeInventoryResponse getItemsFromDcaeInventoryResponse(String responseStr) throws ParseException {
        JSONParser parser = new JSONParser();
        Object obj0 = parser.parse(responseStr);
        JSONObject jsonObj = (JSONObject) obj0;
        JSONArray itemsArray = (JSONArray) jsonObj.get("items");
        JSONObject dcaeServiceType0 = (JSONObject) itemsArray.get(0);
        return JsonUtils.GSON.fromJson(dcaeServiceType0.toString(), DcaeInventoryResponse.class);
    }

    /**
     * DO a query to DCAE to get some Information.
     *
     * @param artifactName The artifact Name
     * @param serviceUuid  The service UUID
     * @param resourceUuid The resource UUID
     * @return The DCAE inventory for the artifact in DcaeInventoryResponse
     * @throws IOException    In case of issues with the stream
     * @throws ParseException In case of issues with the Json parsing
     */
    public DcaeInventoryResponse getDcaeInformation(String artifactName, String serviceUuid, String resourceUuid)
            throws IOException, ParseException, InterruptedException {
        LoggingUtils.setTargetContext("DCAE", "getDcaeInformation");

        int retryInterval = 0;
        int retryLimit = 1;
        if (refProp.getStringValue(DCAE_INVENTORY_RETRY_LIMIT) != null) {
            retryLimit = Integer.valueOf(refProp.getStringValue(DCAE_INVENTORY_RETRY_LIMIT));
        }
        if (refProp.getStringValue(DCAE_INVENTORY_RETRY_INTERVAL) != null) {
            retryInterval = Integer.valueOf(refProp.getStringValue(DCAE_INVENTORY_RETRY_INTERVAL));
        }
        for (int i = 0; i < retryLimit; i++) {
            logger.info(LoggerUtils.METRIC_LOG_MARKER, "Attempt n° {} to contact DCAE inventory", i);
            try (ProducerTemplate producerTemplate = camelContext.createProducerTemplate()) {
                Exchange exchangeResponse = producerTemplate
                        .send("direct:get-dcae-blueprint-inventory", ExchangeBuilder.anExchange(camelContext)
                                .withProperty("blueprintResourceId", resourceUuid)
                                .withProperty("blueprintServiceId", serviceUuid)
                                .withProperty("blueprintName", artifactName)
                                .withProperty("raiseHttpExceptionFlag", false).build());

                if (HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
                        .is2xxSuccessful()) {
                    String dcaeResponse = (String) exchangeResponse.getIn().getBody();
                    int totalCount = getTotalCountFromDcaeInventoryResponse(dcaeResponse);
                    logger.info(LoggerUtils.METRIC_LOG_MARKER,
                          "getDcaeInformation complete: totalCount returned= {}", totalCount);
                    if (totalCount > 0) {
                        logger.info("getDcaeInformation, answer from DCAE inventory: {}", dcaeResponse);
                        LoggingUtils.setResponseContext("0", "Get Dcae Information success", this.getClass().getName());
                        Date startTime = new Date();
                        LoggingUtils.setTimeContext(startTime, new Date());
                        return getItemsFromDcaeInventoryResponse(dcaeResponse);
                    } else {
                        logger.info("Dcae inventory totalCount returned is 0, so waiting {} ms before retrying ...",
                             retryInterval);
                        // wait for a while and try to connect to DCAE again
                        Thread.sleep(retryInterval);
                    }
                }
            }
        }
        logger.warn("Dcae inventory totalCount returned is still 0, after {} attempts, returning NULL", retryLimit);
        return null;
    }
}

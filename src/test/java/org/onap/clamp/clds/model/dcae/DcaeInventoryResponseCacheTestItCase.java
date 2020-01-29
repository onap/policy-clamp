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

package org.onap.clamp.clds.model.dcae;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class DcaeInventoryResponseCacheTestItCase {

    public static DcaeInventoryCache inventoryCache = new DcaeInventoryCache();

    @Autowired
    CamelContext camelContext;

    /**
     * Initialize the responses.
     */
    @BeforeClass
    public static void createExample() {
        DcaeInventoryResponse response1 = new DcaeInventoryResponse();
        response1.setAsdcServiceId("id1");
        response1.setAsdcResourceId("0");
        DcaeInventoryResponse response2 = new DcaeInventoryResponse();
        response2.setAsdcServiceId("id1");
        response2.setAsdcResourceId("1");
        DcaeInventoryResponse response3 = new DcaeInventoryResponse();
        response3.setAsdcServiceId("id1");
        response3.setAsdcResourceId("2");
        DcaeInventoryResponse response4 = new DcaeInventoryResponse();
        response4.setAsdcServiceId("id2");
        response4.setAsdcResourceId("0");
        DcaeInventoryResponse response5 = new DcaeInventoryResponse();
        response5.setAsdcServiceId("id2");
        response5.setAsdcResourceId("1");

        inventoryCache.addDcaeInventoryResponse(response1);
        inventoryCache.addDcaeInventoryResponse(response3);
        inventoryCache.addDcaeInventoryResponse(response2);
        inventoryCache.addDcaeInventoryResponse(response4);
        inventoryCache.addDcaeInventoryResponse(response5);
    }

    @Test
    public void testGetAllLoopIds() {
        assertThat(inventoryCache.getAllLoopIds().size()).isEqualTo(2);
    }

    @Test
    public void testGetAllBlueprintsPerLoopId() {
        int value = 0;
        for (DcaeInventoryResponse inventoryResponse : inventoryCache.getAllBlueprintsPerLoopId("id1")) {
            assertThat(Integer.valueOf(inventoryResponse.getAsdcResourceId())).isEqualTo(value++);
        }

        value = 0;
        for (DcaeInventoryResponse inventoryResponse : inventoryCache.getAllBlueprintsPerLoopId("id2")) {
            assertThat(Integer.valueOf(inventoryResponse.getAsdcResourceId())).isEqualTo(value++);
        }
    }

    @Test
    public void testDcaeInventoryResponse() {
        Exchange exchange = ExchangeBuilder.anExchange(camelContext).build();
        Exchange exchangeResponse = camelContext.createProducerTemplate()
                .send("direct:get-all-dcae-blueprint-inventory", exchange);
        assertThat(exchangeResponse.getIn().getHeader("CamelHttpResponseCode")).isEqualTo(200);
        Set<DcaeInventoryResponse> blueprint = inventoryCache.getAllBlueprintsPerLoopId("testAsdcServiceId");
        assertThat(blueprint.size()).isEqualTo(2);

        DcaeInventoryResponse response1 = new DcaeInventoryResponse();
        response1.setAsdcResourceId("0");
        response1.setTypeName("testTypeName");
        response1.setAsdcServiceId("testAsdcServiceId");
        response1.setBlueprintTemplate("testBlueprintTemplate");
        response1.setTypeId("testtypeId");
        DcaeInventoryResponse response2 = new DcaeInventoryResponse();
        response2.setAsdcResourceId("1");
        response2.setTypeName("testTypeName2");
        response2.setAsdcServiceId("testAsdcServiceId");
        response2.setBlueprintTemplate("testBlueprintTemplate2");
        response2.setTypeId("testtypeId2");

        Set<DcaeInventoryResponse> expectedBlueprint = new HashSet<>();
        expectedBlueprint.add(response1);
        expectedBlueprint.add(response2);

        assertEquals(blueprint, expectedBlueprint);
    }
}

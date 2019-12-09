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

import org.junit.BeforeClass;
import org.junit.Test;

public class DcaeInventoryResponseCacheTest {

    public static DcaeInventoryCache inventoryCache = new DcaeInventoryCache();

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

}

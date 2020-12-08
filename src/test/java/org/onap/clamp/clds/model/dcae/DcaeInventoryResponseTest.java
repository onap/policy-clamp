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

import java.util.TreeSet;

import org.junit.Test;

public class DcaeInventoryResponseTest {

    @Test
    public void testComparator() {
        DcaeInventoryResponse response1 = new DcaeInventoryResponse();
        response1.setAsdcServiceId("id1");
        response1.setAsdcResourceId("0");
        DcaeInventoryResponse response2 = new DcaeInventoryResponse();
        response2.setAsdcServiceId("id2");
        response2.setAsdcResourceId("1");
        DcaeInventoryResponse response3 = new DcaeInventoryResponse();
        response3.setAsdcServiceId("id3");
        response3.setAsdcResourceId("2");
        DcaeInventoryResponse response4 = new DcaeInventoryResponse();
        response4.setAsdcServiceId("id4");
        response4.setAsdcResourceId("3");

        TreeSet<DcaeInventoryResponse> responseSet = new TreeSet<>();
        responseSet.add(response4);
        responseSet.add(response3);
        responseSet.add(response1);
        responseSet.add(response2);

        int value = 0;
        for (DcaeInventoryResponse inventoryResponse : responseSet) {
            assertThat(Integer.valueOf(inventoryResponse.getAsdcResourceId()) == value++).isTrue();
        }
    }
}

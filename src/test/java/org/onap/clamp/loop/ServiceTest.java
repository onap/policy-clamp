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

package org.onap.clamp.loop;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import org.junit.Test;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.loop.service.Service;

public class ServiceTest {

    @Test
    public void equalMethodTest() {
        String serviceStr1 = "{\"name\": \"vLoadBalancerMS\", \"UUID\": \"63cac700-ab9a-4115-a74f-7eac85e3fce0\"}";
        String serviceStr2 = "{\"name\": \"vLoadBalancerMS2\", \"UUID\": \"63cac700-ab9a-4115-a74f-7eac85e3fce0\"}";
        String serviceStr3 = "{\"name\": \"vLoadBalancerMS\",\"UUID\": \"63cac700-ab9a-4115-a74f-7eac85e3fc11\"}";
        String resourceStr = "{\"CP\": {}}";

        Service service1 = new Service(JsonUtils.GSON.fromJson(serviceStr1, JsonObject.class), 
                JsonUtils.GSON.fromJson(resourceStr, JsonObject.class), "1.0");

        Service service2 = new Service(JsonUtils.GSON.fromJson(serviceStr2, JsonObject.class), null, "1.0");

        Service service3 = new Service(JsonUtils.GSON.fromJson(serviceStr3, JsonObject.class), 
                JsonUtils.GSON.fromJson(resourceStr, JsonObject.class), "1.0");

        assertThat(service1.equals(service2)).isEqualTo(true);
        assertThat(service1.equals(service3)).isEqualTo(false);
    }

}

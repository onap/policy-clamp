/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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
 * Modifications copyright (c) 2019 Nokia
 * ===================================================================
 *
 */

package org.onap.clamp.clds.util;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;

import java.util.List;
import org.junit.Test;

public class JsonUtilsTest {

    public static class TestClass extends TestObject {

        String test2;
        TestObject2 object2;

        TestClass(String value1, String value2) {
            super(value1);
            test2 = value2;
        }

        void setObject2(TestObject2 object2) {
            this.object2 = object2;
        }
    }

    private static final JsonObject DEPLOY_PARAMETERS = JsonUtils.GSON.fromJson(
        "{\n"
            + "        \"aaiEnrichmentHost\": \"aai.onap.svc.cluster.local\",\n"
            + "        \"aaiEnrichmentPort\": \"8443\",\n"
            + "        \"enableAAIEnrichment\": true,\n"
            + "        \"dmaap_host\": \"message-router\",\n"
            + "        \"dmaap_port\": \"3904\",\n"
            + "        \"enableRedisCaching\": false,\n"
            + "        \"redisHosts\": \"dcae-redis:6379\",\n"
            + "        \"tag_version\": \"nexus3.onap.org:10001/onap/org.onap.dcaegen2."
            + "deployments.tca-cdap-container:1.1.0\",\n"
            + "        \"consul_host\": \"consul-server\",\n"
            + "        \"consul_port\": \"8500\",\n"
            + "        \"cbs_host\": \"config-binding-service\",\n"
            + "        \"cbs_port\": \"10000\",\n"
            + "        \"external_port\": \"32010\",\n"
            + "        \"policy_id\": \"AUTO_GENERATED_POLICY_ID_AT_SUBMIT\"\n"
            + "      }", JsonObject.class);


    @Test
    public void testGetObjectMapperInstance() {
        assertNotNull(JsonUtils.GSON);
    }

    /**
     * This method test that the security hole in Jackson is not enabled in the default ObjectMapper.
     */
    @Test
    public void testCreateBeanDeserializer() {
        TestClass test = new TestClass("value1", "value2");
        test.setObject2(new TestObject2("test3"));
        Object testObject = JsonUtils.GSON.fromJson("[\"org.onap.clamp.clds.util.JsonUtilsTest$TestClass\""
            + ",{\"test\":\"value1\",\"test2\":\"value2\",\"object2\":[\"org.onap.clamp.clds.util.TestObject2\","
            + "{\"test3\":\"test3\"}]}]", Object.class);
        assertNotNull(testObject);
        assertFalse(testObject instanceof TestObject);
    }


    @Test
    public void shouldReturnJsonValueByName() throws IOException {
        //given
        String modelProperties = ResourceFileUtil
            .getResourceAsString("example/model-properties/custom/modelBpmnPropertiesMultiVF.json");
        JsonElement globalElement = JsonUtils.GSON.fromJson(modelProperties, JsonObject.class).get("global");

        //when
        String locationName = JsonUtils.getStringValueByName(globalElement, "location");
        String timeoutValue = JsonUtils.getStringValueByName(globalElement, "timeout");

        //then
        assertThat(locationName).isEqualTo("SNDGCA64");
        assertThat(timeoutValue).isEqualTo("500");
    }

    @Test
    public void shouldReturnJsonObjectByPropertyName() throws IOException {
        //given
        String modelProperties = ResourceFileUtil
            .getResourceAsString("example/model-properties/custom/modelBpmnPropertiesMultiVF.json");
        JsonElement globalElement = JsonUtils.GSON.fromJson(modelProperties, JsonObject.class).get("global");

        //when
        JsonObject deployParameters = JsonUtils.getJsonObjectByName(globalElement, "deployParameters");

        //then
        assertThat(deployParameters).isEqualToComparingFieldByField(DEPLOY_PARAMETERS);
    }

    @Test
    public void shouldReturnJsonValuesByPropertyName() throws IOException {
        //given
        String modelProperties = ResourceFileUtil
            .getResourceAsString("example/model-properties/custom/modelBpmnPropertiesMultiVF.json");
        JsonElement globalElement = JsonUtils.GSON.fromJson(modelProperties, JsonObject.class).get("global");

        //when
        List<String> vfs = JsonUtils.getStringValuesByName(globalElement, "vf");

        //then
        assertThat(vfs).containsExactly(
            "6c7aaec2-59eb-41d9-8681-b7f976ab668d",
            "8sadsad0-a98s-6a7s-fd12-sadji9sa8d12",
            "8sfd71ad-a90d-asd9-as87-8a7sd81adsaa"
        );
    }


    @Test
    public void shouldReturnJsonValueAsInteger() throws IOException {
        //given
        String modelProperties = ResourceFileUtil
            .getResourceAsString("example/model-properties/custom/modelBpmnPropertiesMultiVF.json");
        JsonElement globalElement = JsonUtils.GSON.fromJson(modelProperties, JsonObject.class).get("global");

        //when
        Integer timeoutValue = JsonUtils.getIntValueByName(globalElement, "timeout");

        //then
        assertThat(timeoutValue).isEqualTo(500);
    }
}

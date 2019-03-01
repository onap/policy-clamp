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
import static org.junit.Assert.assertNotNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Random;

import org.junit.Test;
import org.onap.clamp.loop.log.LogType;
import org.onap.clamp.loop.log.LoopLog;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;
import org.onap.clamp.clds.util.JsonUtils;

public class LoopToJsonTest {

    private Gson gson = new Gson();

    private OperationalPolicy getOperationalPolicy(String configJson, String name) {
        return new OperationalPolicy(name, null, gson.fromJson(configJson, JsonObject.class));
    }

    private Loop getLoop(String name, String svgRepresentation, String blueprint, String globalPropertiesJson,
        String dcaeId, String dcaeUrl, String dcaeBlueprintId) {
        Loop loop = new Loop(name, blueprint, svgRepresentation);
        loop.setGlobalPropertiesJson(new Gson().fromJson(globalPropertiesJson, JsonObject.class));
        loop.setLastComputedState(LoopState.DESIGN);
        loop.setDcaeDeploymentId(dcaeId);
        loop.setDcaeDeploymentStatusUrl(dcaeUrl);
        loop.setDcaeBlueprintId(dcaeBlueprintId);
        return loop;
    }

    private MicroServicePolicy getMicroServicePolicy(String name, String jsonRepresentation, String policyTosca,
        String jsonProperties, boolean shared) {
        MicroServicePolicy µService = new MicroServicePolicy(name, policyTosca, shared,
            gson.fromJson(jsonRepresentation, JsonObject.class), new HashSet<>());
        µService.setProperties(new Gson().fromJson(jsonProperties, JsonObject.class));

        return µService;
    }

    private LoopLog getLoopLog(LogType type, String message) {
        LoopLog log = new LoopLog();
        log.setLogType(type);
        log.setMessage(message);
        log.setId(Long.valueOf(new Random().nextInt()));
        return log;
    }

    @Test
    public void LoopGsonTest() {
        Loop loopTest = getLoop("ControlLoopTest", "<xml></xml>", "yamlcontent", "{\"testname\":\"testvalue\"}",
            "123456789", "https://dcaetest.org", "UUID-blueprint");
        OperationalPolicy opPolicy = this.getOperationalPolicy("{\"type\":\"GUARD\"}", "GuardOpPolicyTest");
        loopTest.addOperationalPolicy(opPolicy);
        MicroServicePolicy microServicePolicy = getMicroServicePolicy("configPolicyTest", "{\"configtype\":\"json\"}",
            "YamlContent", "{\"param1\":\"value1\"}", true);
        loopTest.addMicroServicePolicy(microServicePolicy);
        LoopLog loopLog = getLoopLog(LogType.INFO, "test message");
        loopTest.addLog(loopLog);

        String jsonSerialized = JsonUtils.GSON_JPA_MODEL.toJson(loopTest);
        assertThat(jsonSerialized).isNotNull().isNotEmpty();
        System.out.println(jsonSerialized);
        Loop loopTestDeserialized = JsonUtils.GSON_JPA_MODEL.fromJson(jsonSerialized, Loop.class);
        assertNotNull(loopTestDeserialized);
        assertThat(loopTestDeserialized).isEqualToIgnoringGivenFields(loopTest, "svgRepresentation", "blueprint");

        //svg and blueprint not exposed so wont be deserialized
        assertThat(loopTestDeserialized.getBlueprint()).isEqualTo(null);
        assertThat(loopTestDeserialized.getSvgRepresentation()).isEqualTo(null);

        assertThat(loopTestDeserialized.getOperationalPolicies()).containsExactly(opPolicy);
        assertThat(loopTestDeserialized.getMicroServicePolicies()).containsExactly(microServicePolicy);
        assertThat(loopTestDeserialized.getLoopLogs()).containsExactly(loopLog);
        assertThat((LoopLog) loopTestDeserialized.getLoopLogs().toArray()[0]).isEqualToIgnoringGivenFields(loopLog,
            "loop");
    }
}

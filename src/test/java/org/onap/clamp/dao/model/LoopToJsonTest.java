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

package org.onap.clamp.dao.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import com.google.gson.Gson;

import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.onap.clamp.clds.util.JsonUtils;

public class LoopToJsonTest {

    private OperationalPolicy getOperationalPolicy(String configJson, String name) {
        OperationalPolicy opPolicy = new OperationalPolicy();
        opPolicy.setName(name);
        opPolicy.setConfigurationsJson(new Gson().fromJson(configJson, Map.class));
        return opPolicy;
    }

    private Loop getLoop(String name, String svgRepresentation, String blueprint, String globalPropertiesJson,
        String dcaeId, String dcaeUrl) {
        Loop loop = new Loop();
        loop.setName(name);
        loop.setSvgRepresentation(svgRepresentation);
        loop.setBlueprint(blueprint);
        loop.setGlobalPropertiesJson(new Gson().fromJson(globalPropertiesJson, Map.class));
        loop.setLastComputedState(LoopState.DESIGN);
        loop.setDcaeDeploymentId(dcaeId);
        loop.setDcaeDeploymentStatusUrl(dcaeUrl);
        return loop;
    }

    private MicroServicePolicy getMicroServicePolicy(String name, String jsonRepresentation, String policyTosca,
        String jsonProperties, boolean shared) {
        MicroServicePolicy µService = new MicroServicePolicy();
        µService.setJsonRepresentation(new Gson().fromJson(jsonRepresentation, Map.class));
        µService.setPolicyTosca(policyTosca);
        µService.setProperties(new Gson().fromJson(jsonProperties, Map.class));
        µService.setShared(shared);

        µService.setName(name);
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
            "123456789", "https://dcaetest.org");
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
        assertThat(loopTestDeserialized).isEqualToComparingFieldByField(loopTest);
        assertThat(loopTestDeserialized.getOperationalPolicies()).containsExactly(opPolicy);
        assertThat(loopTestDeserialized.getMicroServicePolicies()).containsExactly(microServicePolicy);
        assertThat(loopTestDeserialized.getLoopLogs()).containsExactly(loopLog);
        assertThat((LoopLog) loopTestDeserialized.getLoopLogs().toArray()[0]).isEqualToIgnoringGivenFields(loopLog,
            "loop");
    }
}

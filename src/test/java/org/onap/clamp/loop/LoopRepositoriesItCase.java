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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.time.Instant;

import java.util.HashSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.Application;
import org.onap.clamp.loop.log.LogType;
import org.onap.clamp.loop.log.LoopLog;
import org.onap.clamp.loop.log.LoopLogRepository;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.microservice.MicroservicePolicyService;
import org.onap.clamp.policy.operational.OperationalPolicy;
import org.onap.clamp.policy.operational.OperationalPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class LoopRepositoriesItCase {

    private Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    @Autowired
    private LoopsRepository loopRepository;

    @Autowired
    private MicroservicePolicyService microServicePolicyService;

    @Autowired
    private OperationalPolicyService operationalPolicyService;

    @Autowired
    private LoopLogRepository loopLogRepository;

    private OperationalPolicy getOperationalPolicy(String configJson, String name) {
        return new OperationalPolicy(name, null, new Gson().fromJson(configJson, JsonObject.class));
    }

    private Loop getLoop(String name, String svgRepresentation, String blueprint, String globalPropertiesJson,
        String dcaeId, String dcaeUrl, String dcaeBlueprintId) {
        Loop loop = new Loop();
        loop.setName(name);
        loop.setSvgRepresentation(svgRepresentation);
        loop.setBlueprint(blueprint);
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
        return log;
    }

    @Test
    @Transactional
    public void CrudTest() {
        Loop loopTest = getLoop("ControlLoopTest", "<xml></xml>", "yamlcontent", "{\"testname\":\"testvalue\"}",
            "123456789", "https://dcaetest.org", "UUID-blueprint");
        OperationalPolicy opPolicy = this.getOperationalPolicy("{\"type\":\"GUARD\"}", "GuardOpPolicyTest");
        loopTest.addOperationalPolicy(opPolicy);
        MicroServicePolicy microServicePolicy = getMicroServicePolicy("configPolicyTest", "{\"configtype\":\"json\"}",
            "YamlContent", "{\"param1\":\"value1\"}", true);
        loopTest.addMicroServicePolicy(microServicePolicy);
        LoopLog loopLog = getLoopLog(LogType.INFO, "test message");
        loopTest.addLog(loopLog);

        // Attemp to save into the database the entire loop
        Loop loopInDb = loopRepository.save(loopTest);
        assertThat(loopInDb).isNotNull();
        assertThat(loopInDb.getName()).isEqualTo("ControlLoopTest");
        // Now set the ID in the previous model so that we can compare the objects
        loopLog.setId(((LoopLog) loopInDb.getLoopLogs().toArray()[0]).getId());

        assertThat(loopInDb).isEqualToComparingFieldByField(loopTest);
        assertThat(loopRepository.existsById(loopTest.getName())).isEqualTo(true);
        assertThat(operationalPolicyService.isExisting(opPolicy.getName())).isEqualTo(true);
        assertThat(microServicePolicyService.isExisting(microServicePolicy.getName())).isEqualTo(true);
        assertThat(loopLogRepository.existsById(loopLog.getId())).isEqualTo(true);

        // Now attempt to read from database
        Loop loopInDbRetrieved = loopRepository.findById(loopTest.getName()).get();
        assertThat(loopInDbRetrieved).isEqualToComparingFieldByField(loopTest);
        assertThat((LoopLog) loopInDbRetrieved.getLoopLogs().toArray()[0]).isEqualToComparingFieldByField(loopLog);
        assertThat((OperationalPolicy) loopInDbRetrieved.getOperationalPolicies().toArray()[0])
            .isEqualToComparingFieldByField(opPolicy);
        assertThat((MicroServicePolicy) loopInDbRetrieved.getMicroServicePolicies().toArray()[0])
            .isEqualToComparingFieldByField(microServicePolicy);

        // Attempt an update
        ((LoopLog) loopInDbRetrieved.getLoopLogs().toArray()[0]).setLogInstant(Instant.now());
        loopRepository.save(loopInDbRetrieved);
        Loop loopInDbRetrievedUpdated = loopRepository.findById(loopTest.getName()).get();
        assertThat((LoopLog) loopInDbRetrievedUpdated.getLoopLogs().toArray()[0])
            .isEqualToComparingFieldByField(loopInDbRetrieved.getLoopLogs().toArray()[0]);

        // Attempt to delete the object and check it has well been cascaded
        loopRepository.delete(loopInDbRetrieved);
        assertThat(loopRepository.existsById(loopTest.getName())).isEqualTo(false);
        assertThat(operationalPolicyService.isExisting(opPolicy.getName())).isEqualTo(false);
        assertThat(microServicePolicyService.isExisting(microServicePolicy.getName())).isEqualTo(false);
        assertThat(loopLogRepository.existsById(loopLog.getId())).isEqualTo(false);

    }
}

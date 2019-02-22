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

package org.onap.clamp.it.dao.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.Application;
import org.onap.clamp.dao.LoopsRepository;
import org.onap.clamp.dao.model.LogType;
import org.onap.clamp.dao.model.Loop;
import org.onap.clamp.dao.model.LoopLog;
import org.onap.clamp.dao.model.LoopState;
import org.onap.clamp.dao.model.MicroServicePolicy;
import org.onap.clamp.dao.model.OperationalPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class LoopItCase {

    @Autowired
    private LoopsRepository loopRepository;

    @Test
    public void DecodeEncodeTest() {
        Loop loopTest = new Loop();
        loopTest.setName("ClosedLoopTest");
        loopTest.setSvgRepresentation("representation");
        loopTest.setBlueprint("blueprint");
        loopTest.setGlobalPropertiesJson(new Gson().fromJson("{\"testName\":\"testValue\"}", Map.class));
        loopTest.setLastComputedState(LoopState.DESIGN);
        loopTest.setBlueprint("yaml");

        OperationalPolicy opPolicy = new OperationalPolicy();
        opPolicy.setName("OpPolicyTest");
        opPolicy.setConfigurationsJson(new Gson().fromJson("{\"testname\":\"testvalue\"}", Map.class));
        opPolicy.setLoop(loopTest);
        loopTest.addOperationalPolicy(opPolicy);

        MicroServicePolicy µService = new MicroServicePolicy();
        µService.setJsonRepresentation(new Gson().fromJson("{\"testrepresentation\":\"value\"}", Map.class));
        µService.setPolicyTosca("tosca");
        µService.setProperties(new Gson().fromJson("{\"testparam\":\"testvalue\"}", Map.class));
        µService.setShared(true);

        µService.setName("ConfigPolicyTest");
        loopTest.addMicroServicePolicy(µService);

        LoopLog log = new LoopLog();
        log.setLogType(LogType.INFO);
        log.setMessage("test message");

        loopTest.addLog(log);

        Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
        String json = gson.toJson(loopTest);
        assertNotNull(json);
        Loop loopTestDecoded = gson.fromJson(json, Loop.class);
        assertNotNull(loopTestDecoded);
        Loop loop = gson.fromJson(json, Loop.class);
        assertNotNull(loop);

        Loop loopInDb = loopRepository.save(loopTest);
        assertNotNull(loopInDb);
        assertTrue(loopRepository.findById(loopInDb.getName()).get().getName().equals("ClosedLoopTest"));
    }
}

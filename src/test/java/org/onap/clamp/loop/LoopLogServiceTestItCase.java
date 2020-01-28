/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Huawei Technologies Co., Ltd.
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

import java.util.Set;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.Application;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.loop.log.LogType;
import org.onap.clamp.loop.log.LoopLog;
import org.onap.clamp.loop.log.LoopLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class LoopLogServiceTestItCase {

    private static final String EXAMPLE_LOOP_NAME = "ClosedLoopTest";
    private static final String EXAMPLE_JSON = "{\"testName\":\"testValue\"}";
    private static final String CLAMP_COMPONENT = "CLAMP";
    private static final String SAMPLE_LOG_MESSAGE = "Sample log";
    private static final String BLUEPRINT = "blueprint";
    private static final String SVG_REPRESENTATION = "representation";

    @Autowired
    LoopService loopService;

    @Autowired
    LoopsRepository loopsRepository;

    @Autowired
    LoopLogService loopLogService;

    private void saveTestLoopToDb() {
        Loop testLoop = new Loop(EXAMPLE_LOOP_NAME, SVG_REPRESENTATION);
        testLoop.setGlobalPropertiesJson(JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class));
        loopService.saveOrUpdateLoop(testLoop);
    }

    @Test
    @Transactional
    public void testAddLog() {
        saveTestLoopToDb();
        Loop loop = loopService.getLoop(EXAMPLE_LOOP_NAME);
        loopLogService.addLog(SAMPLE_LOG_MESSAGE, "INFO", loop);
        Set<LoopLog> loopLogs = loop.getLoopLogs();
        assertThat(loopLogs).hasSize(1);
        LoopLog loopLog = loopLogs.iterator().next();
        assertThat(loopLog.getMessage()).isEqualTo(SAMPLE_LOG_MESSAGE);
    }

    @Test
    @Transactional
    public void testLoopLog() {
        LoopLog log = new LoopLog();
        Long id = Long.valueOf(100);
        log.setId(id);
        log.setLogComponent(CLAMP_COMPONENT);
        log.setLogType(LogType.INFO);
        log.setMessage(SAMPLE_LOG_MESSAGE);
        Loop testLoop = new Loop(EXAMPLE_LOOP_NAME, SVG_REPRESENTATION);
        log.setLoop(testLoop);
        assertThat(log.getMessage()).isEqualTo(SAMPLE_LOG_MESSAGE);
        assertThat(log.getLogType()).isEqualTo(LogType.INFO);
        assertThat(log.getLogComponent()).isEqualTo(CLAMP_COMPONENT);
        assertThat(log.getId()).isEqualTo(id);
        assertThat(log.getLoop()).isEqualTo(testLoop);
    }
}
/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.clamp.clds.it.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.config.sdc.SdcControllersConfiguration;
import org.onap.clamp.clds.config.sdc.SdcSingleControllerConfiguration;
import org.onap.clamp.clds.exception.sdc.controller.SdcParametersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * This class tests the SDC Controller config.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SdcControllersConfigurationItCase {

    @Autowired
    private SdcControllersConfiguration sdcControllersConfiguration;

    public final void loadFile(String fileName) throws IOException {
        ReflectionTestUtils.setField(sdcControllersConfiguration, "sdcControllerFile", fileName);
        sdcControllersConfiguration.loadConfiguration();
    }

    @Test
    public void testGetAllDefinedControllers() throws IOException {
        loadFile("classpath:/clds/sdc-controllers-config.json");
        Map<String, SdcSingleControllerConfiguration> mapResult = sdcControllersConfiguration
            .getAllDefinedControllers();
        assertTrue(mapResult.size() == 2);
        assertEquals("sdc-controller1", mapResult.get("sdc-controller1").getSdcControllerName());
        assertEquals("sdc-controller2", mapResult.get("sdc-controller2").getSdcControllerName());
    }

    @Test
    public void testGetSdcSingleControllerConfiguration() throws IOException {
        loadFile("classpath:/clds/sdc-controllers-config.json");
        assertEquals("sdc-controller1",
            sdcControllersConfiguration.getSdcSingleControllerConfiguration("sdc-controller1").getSdcControllerName());
        assertEquals("sdc-controller2",
            sdcControllersConfiguration.getSdcSingleControllerConfiguration("sdc-controller2").getSdcControllerName());
    }

    @Test(expected = JsonSyntaxException.class)
    public void testBadJsonLoading() throws IOException {
        loadFile("classpath:/clds/sdc-controllers-config-bad.json");
        fail("Should have raised an exception");
    }

    @Test(expected = SdcParametersException.class)
    public void testMissingParamInJsonLoading() throws IOException {
        loadFile("classpath:/clds/sdc-controllers-config-missing-param.json");
        sdcControllersConfiguration.getAllDefinedControllers();
        fail("Should have raised an exception");
    }
}

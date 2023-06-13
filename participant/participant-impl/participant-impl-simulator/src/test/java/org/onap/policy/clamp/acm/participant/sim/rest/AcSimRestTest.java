/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.participant.sim.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.participant.sim.comm.CommonTestData;
import org.onap.policy.clamp.acm.participant.sim.main.handler.AutomationCompositionElementHandler;
import org.onap.policy.clamp.acm.participant.sim.model.InternalData;
import org.onap.policy.clamp.acm.participant.sim.model.InternalDatas;
import org.onap.policy.clamp.acm.participant.sim.model.SimConfig;
import org.onap.policy.clamp.acm.participant.sim.parameters.ParticipantSimParameters;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@WebMvcTest(value = SimulatorController.class)
@Import({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@EnableConfigurationProperties(value = ParticipantSimParameters.class)
class AcSimRestTest {

    private static final Coder CODER = new StandardCoder();
    private static final String CONFIG_URL = "/v2/parameters";
    private static final String INSTANCE_URL = "/v2/instances";
    private static final String DATAS_URL = "/v2/datas";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AutomationCompositionElementHandler automationCompositionElementHandler;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void mockServiceClass() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
    }

    @Test
    void testgetConfig() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.get(CONFIG_URL).accept(MediaType.APPLICATION_JSON_VALUE);

        doReturn(new SimConfig()).when(automationCompositionElementHandler).getConfig();

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.primeTimerMs", is(100)));
    }

    @Test
    void testsetConfig() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.put(CONFIG_URL).accept(MediaType.APPLICATION_JSON_VALUE)
                .content(CODER.encode(new SimConfig())).contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().isOk());
    }

    @Test
    void testgetAutomationCompositions() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.get(INSTANCE_URL).accept(MediaType.APPLICATION_JSON_VALUE);

        var automationCompositions = new AutomationCompositions();
        automationCompositions.getAutomationCompositionList().add(CommonTestData.getTestAutomationComposition());

        doReturn(automationCompositions).when(automationCompositionElementHandler).getAutomationCompositions();

        var result = mockMvc.perform(requestBuilder).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andReturn();
        var body = result.getResponse().getContentAsString();
        var acsResult = CODER.decode(body, AutomationCompositions.class);
        assertEquals(automationCompositions.getAutomationCompositionList().get(0).getInstanceId(),
                acsResult.getAutomationCompositionList().get(0).getInstanceId());
    }

    @Test
    void testgetDatas() throws Exception {
        var internalDatas = new InternalDatas();
        var internalData = new InternalData();
        internalData.setAutomationCompositionId(UUID.randomUUID());
        internalDatas.getList().add(internalData);

        doReturn(internalDatas).when(automationCompositionElementHandler).getDataList();

        var requestBuilder = MockMvcRequestBuilders.get(DATAS_URL).accept(MediaType.APPLICATION_JSON_VALUE);
        var result = mockMvc.perform(requestBuilder).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andReturn();
        var body = result.getResponse().getContentAsString();
        var acsResult = CODER.decode(body, InternalDatas.class);
        assertEquals(internalData.getAutomationCompositionId(),
                acsResult.getList().get(0).getAutomationCompositionId());
    }

    @Test
    void testsetDatas() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.put(DATAS_URL).accept(MediaType.APPLICATION_JSON_VALUE)
                .content(CODER.encode(new InternalData())).contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().isOk());
    }
}

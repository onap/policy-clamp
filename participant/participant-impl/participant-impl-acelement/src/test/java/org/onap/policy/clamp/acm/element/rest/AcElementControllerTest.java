/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.element.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.element.main.parameters.AcElement;
import org.onap.policy.clamp.acm.element.main.rest.AcElementController;
import org.onap.policy.clamp.acm.element.service.ConfigService;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementConfig;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;


@ExtendWith(SpringExtension.class)
@WebMvcTest(value = AcElementController.class)
@EnableConfigurationProperties(value = AcElement.class)
class AcElementControllerTest {

    private static final Coder CODER = new StandardCoder();
    private static final String ELEMENT_CONFIG_YAML = "src/test/resources/config.json";
    private static final String RETRIEVE_CONFIG = "/v2/config";
    private static final String ACTIVATE_CONFIG = "/v2/activate";
    private static final String DEACTIVATE_CONFIG = "/v2/deactivate";
    private static ElementConfig config;


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConfigService configService;

    @Autowired
    private WebApplicationContext context;

    /**
     * Read input element config json.
     * @throws CoderException in case of error.
     */
    @BeforeAll
    static void setupParams() throws CoderException {
        config = CODER.decode(new File(ELEMENT_CONFIG_YAML), ElementConfig.class);
    }

    /**
     * Mock service layer in Controller.
     */
    @BeforeEach
    void mockServiceClass() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
        when(configService.getElementConfig()).thenReturn(config);
    }

    /**
     * Test endpoint for retrieving element config.
     * @throws Exception in case of error.
     */
    @Test
    void retrieveElementConfig() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.get(RETRIEVE_CONFIG)
                .accept(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.elementType", is("STARTER")));
    }

    /**
     * Test endpoint for activating element config.
     * @throws Exception in case of error.
     */
    @Test
    void activateConfig() throws Exception {
        //Mocking successful activation of element config
        doNothing().when(configService).activateElement(config);

        var requestBuilder = MockMvcRequestBuilders.post(ACTIVATE_CONFIG)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(getElementConfigJson())
                .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().isCreated());

        doThrow(new AutomationCompositionRuntimeException(Response.Status.CONFLICT, "service manager already running"))
                .when(configService).activateElement(any());

        //Activate Invalid config, expects HTTP status CONFLICT
        requestBuilder = MockMvcRequestBuilders.post(ACTIVATE_CONFIG).accept(MediaType.APPLICATION_JSON_VALUE)
                .content(getInvalidJson())
                .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().isConflict())
                .andExpect(result -> assertEquals("service manager already running",
                        result.getResolvedException().getMessage()));
    }

    /**
     * Test endpoint for deactivating element config.
     * @throws Exception in case of error.
     */
    @Test
    void deActivateConfig() throws Exception {

        //Mocking successful deactivation of element config
        doNothing().when(configService).deleteConfig();

        var requestBuilder = MockMvcRequestBuilders.delete(DEACTIVATE_CONFIG);

        mockMvc.perform(requestBuilder).andExpect(status().isNoContent());
    }

    private String getInvalidJson() {
        return new JSONObject().toString();
    }

    private String getElementConfigJson() throws IOException {
        return FileUtils.readFileToString(new File(ELEMENT_CONFIG_YAML), StandardCharsets.UTF_8);
    }

}

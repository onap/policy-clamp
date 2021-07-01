/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.kubernetes.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.controlloop.participant.kubernetes.controller.ChartController;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.ChartList;
import org.onap.policy.clamp.controlloop.participant.kubernetes.parameters.ParticipantK8sParameters;
import org.onap.policy.clamp.controlloop.participant.kubernetes.service.ChartService;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;


@ExtendWith(SpringExtension.class)
@WebMvcTest(value = ChartController.class)
@EnableConfigurationProperties(value = ParticipantK8sParameters.class)
public class ChartControllerTest {

    private static final Coder CODER = new StandardCoder();
    private static final String CHART_INFO_YAML = "src/test/resources/ChartList.json";
    private static List<ChartInfo> charts;
    private static String DEFAULT_CHART_URL = "/helm/charts";
    private static String INSTALL_CHART_URL = "/helm/install";
    private static String UNINSTALL_CHART_URL = "/helm/uninstall/";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChartService chartService;

    @Autowired
    private WebApplicationContext context;

    /**
     * Read input chart info json.
     * @throws Exception incase of error.
     */
    @BeforeAll
    public static void setupParams() throws CoderException {
        charts = (List<ChartInfo>) CODER.decode(new File(CHART_INFO_YAML), ChartList.class).getCharts();
    }

    /**
     * Mock service layer in Controller.
     * @throws Exception incase of error.
     */
    @BeforeEach
    public void mockServiceClass() {
        when(chartService.getAllCharts()).thenReturn(charts);
        when(chartService.getChart(charts.get(0).getChartName(), charts.get(0).getVersion()))
            .thenReturn(charts.get(0));

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
    }

    /**
     * Test endpoint for retrieving all charts.
     * @throws Exception incase of error.
     */
    @Test
    public void retrieveAllCharts() throws Exception {
        RequestBuilder requestBuilder;
        requestBuilder = MockMvcRequestBuilders.get(DEFAULT_CHART_URL).accept(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.charts.[0].chartName", is("HelloWorld")));
    }

    /**
     * Test endpoint for installing a chart.
     * @throws Exception incase of error.
     */
    @Test
    public void installChart() throws Exception {
        RequestBuilder requestBuilder;

        //Mocking successful installation for void install method
        doNothing().when(chartService).installChart(charts.get(0));

        requestBuilder = MockMvcRequestBuilders.post(INSTALL_CHART_URL).accept(MediaType.APPLICATION_JSON_VALUE)
            .content(getInstallationJson(charts.get(0).getChartName(), charts.get(0).getVersion()))
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().isCreated());

        //Install Invalid chart, expects HTTP status NOT_FOUND
        requestBuilder = MockMvcRequestBuilders.post(INSTALL_CHART_URL).accept(MediaType.APPLICATION_JSON_VALUE)
            .content(getInstallationJson("invalidName", "invalidVersion"))
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());
    }

    /**
     * Test endpoint for uninstalling a chart.
     * @throws Exception incase of error.
     */
    @Test
    public void uninstallChart() throws Exception {
        RequestBuilder requestBuilder;

        //Mocking successful scenario for void uninstall method
        doNothing().when(chartService).uninstallChart(charts.get(0));

        requestBuilder = MockMvcRequestBuilders.delete(UNINSTALL_CHART_URL + charts.get(0).getChartName()
            + "/" + charts.get(0).getVersion()).accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().isNoContent());

        //Invalid chart
        requestBuilder = MockMvcRequestBuilders.delete(UNINSTALL_CHART_URL + "invalidName"
            + "/" + "invalidVersion").accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());
    }

    /**
     * Test endpoint for chart onboarding.
     * @throws Exception incase of error.
     */
    @Test
    public void onboardChart() throws Exception {
        RequestBuilder requestBuilder;
        MockMultipartFile chartFile = new MockMultipartFile("chart", "hello.tgz",
            MediaType.TEXT_PLAIN_VALUE, "Dummy data".getBytes());

        MockMultipartFile overrideFile = new MockMultipartFile("values", "values.yaml",
            MediaType.TEXT_PLAIN_VALUE, "Dummy data".getBytes());

        //Mocking successful scenario for void uninstall method
        when(chartService.saveChart(charts.get(0), chartFile, null)).thenReturn(charts.get(0));

        requestBuilder = MockMvcRequestBuilders.multipart(DEFAULT_CHART_URL)
            .file(chartFile).file(overrideFile).param("info", getChartInfoJson());

        mockMvc.perform(requestBuilder).andExpect(status().isOk());
    }

    /**
     * Test endpoint for deleting a chart.
     * @throws Exception incase of error.
     */
    @Test
    public void deleteChart() throws Exception {
        RequestBuilder requestBuilder;

        //Mocking successful scenario for void uninstall method
        doNothing().when(chartService).deleteChart(charts.get(0));

        requestBuilder = MockMvcRequestBuilders.delete(DEFAULT_CHART_URL + "/" + charts.get(0).getChartName()
            + "/" + charts.get(0).getVersion()).accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().isNoContent());
        //Invalid chart
        requestBuilder = MockMvcRequestBuilders.delete(UNINSTALL_CHART_URL + "invalidName"
            + "/" + "invalidVersion").accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());

    }

    private String getInstallationJson(String name, String version) {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", name);
        jsonObj.put("version", version);
        return jsonObj.toString();
    }

    private String getChartInfoJson() {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("chartName", charts.get(0).getChartName());
        jsonObj.put("version", charts.get(0).getVersion());
        jsonObj.put("namespace", charts.get(0).getNamespace());
        jsonObj.put("repository", charts.get(0).getRepository());
        jsonObj.put("releaseName", charts.get(0).getReleaseName());
        return jsonObj.toString();
    }

}

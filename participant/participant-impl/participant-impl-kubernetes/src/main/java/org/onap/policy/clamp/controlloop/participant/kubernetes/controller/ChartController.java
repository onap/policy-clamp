/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2021-2022 Nordix Foundation. All rights reserved.
 * ======================================================================
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
 * ========================LICENSE_END===================================
 */

package org.onap.policy.clamp.controlloop.participant.kubernetes.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.util.ArrayList;
import org.onap.policy.clamp.controlloop.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.ChartList;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.HelmRepository;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.InstallationInfo;
import org.onap.policy.clamp.controlloop.participant.kubernetes.service.ChartService;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController("chartController")
@ConditionalOnExpression("${chart.api.enabled:false}")
@RequestMapping("helm")
@Api(tags = {"k8s-participant"})
public class ChartController {

    @Autowired
    private ChartService chartService;

    private static final StandardCoder CODER = new StandardCoder();

    /**
     * REST endpoint to get all the charts.
     *
     * @return List of charts installed
     */
    @GetMapping(path = "/charts", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Return all Charts")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "chart List")})
    public ResponseEntity<ChartList> getAllCharts() {
        return new ResponseEntity<>(ChartList.builder().charts(new ArrayList<>(chartService.getAllCharts())).build(),
                HttpStatus.OK);
    }

    /**
     * REST endpoint to install a helm chart.
     *
     * @param info Info of the chart to be installed
     * @return Status of the install operation
     * @throws ServiceException in case of error
     * @throws IOException in case of IO error
     */
    @PostMapping(path = "/install", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Install the chart")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "chart Installed")})
    public ResponseEntity<Object> installChart(@RequestBody InstallationInfo info)
            throws ServiceException, IOException {
        ChartInfo chart = chartService.getChart(info.getName(), info.getVersion());
        if (chart == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        chartService.installChart(chart);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * REST endpoint to uninstall a specific chart.
     *
     * @param name name of the chart
     * @param version version of the chart
     * @return Status of operation
     * @throws ServiceException in case of error.
     */
    @DeleteMapping(path = "/uninstall/{name}/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Uninstall the Chart")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "chart Uninstalled")})
    public ResponseEntity<Object> uninstallChart(@PathVariable("name") String name,
            @PathVariable("version") String version) throws ServiceException {
        ChartInfo chart = chartService.getChart(name, version);
        if (chart == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        chartService.uninstallChart(chart);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * REST endpoint to onboard a chart.
     *
     * @param chartFile Multipart file for the helm chart
     * @param infoJson AppInfo of the chart
     * @param overrideFile the file for overriding the chart
     * @return Status of onboard operation
     * @throws ServiceException in case of error
     * @throws IOException in case of IO error
     */
    @PostMapping(path = "/onboard/chart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Onboard the Chart")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Chart Onboarded")})
    public ResponseEntity<String> onboardChart(@RequestPart("chart") MultipartFile chartFile,
            @RequestParam(name = "values", required = false) MultipartFile overrideFile,
            @RequestParam("info") String infoJson) throws ServiceException, IOException {

        ChartInfo info;
        try {
            info = CODER.decode(infoJson, ChartInfo.class);
        } catch (CoderException e) {
            throw new ServiceException("Error parsing the chart information", e);
        }

        chartService.saveChart(info, chartFile, overrideFile);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * REST endpoint to delete a specific helm chart.
     *
     * @param name name of the chart
     * @param version version of the chart
     * @return Status of operation
     */
    @DeleteMapping(path = "/chart/{name}/{version}")
    @ApiOperation(value = "Delete the chart")
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Chart Deleted")})
    public ResponseEntity<Object> deleteChart(@PathVariable("name") String name,
            @PathVariable("version") String version) {

        ChartInfo chart = chartService.getChart(name, version);
        if (chart == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        chartService.deleteChart(chart);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * REST endpoint to configure a helm Repository.
     *
     * @param repo Helm repository to be configured
     * @return Status of the operation
     * @throws ServiceException in case of error
     * @throws IOException in case of IO error
     */
    @PostMapping(path = "/repo", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Configure helm repository")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Repository added")})
    public ResponseEntity<Object> configureRepo(@RequestBody String repo)
            throws ServiceException, IOException {
        HelmRepository repository;
        try {
            repository = CODER.decode(repo, HelmRepository.class);
        } catch (CoderException e) {
            throw new ServiceException("Error parsing the repository information", e);
        }
        if (chartService.configureRepository(repository)) {
            return new ResponseEntity<>(HttpStatus.CREATED);
        }
        return new ResponseEntity<>(HttpStatus.CONFLICT);
    }
}

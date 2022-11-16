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

package org.onap.policy.clamp.acm.participant.kubernetes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartList;
import org.onap.policy.clamp.acm.participant.kubernetes.models.HelmRepository;
import org.onap.policy.clamp.acm.participant.kubernetes.models.InstallationInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.service.ChartService;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@RequiredArgsConstructor
@RestController("chartController")
@ConditionalOnExpression("${chart.api.enabled:false}")
@RequestMapping("helm")
@Tag(name = "k8s-participant")
public class ChartController {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ChartService chartService;

    private static final StandardCoder CODER = new StandardCoder();

    /**
     * REST endpoint to get all the charts.
     *
     * @return List of charts installed
     */
    @GetMapping(path = "/charts", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Return all Charts")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "chart List")})
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
    @Operation(summary = "Install the chart")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "chart Installed")})
    public ResponseEntity<Void> installChart(@RequestBody InstallationInfo info)
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
    @Operation(summary = "Uninstall the Chart")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "chart Uninstalled")})
    public ResponseEntity<Void> uninstallChart(@PathVariable("name") String name,
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
    @Operation(summary = "Onboard the Chart")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Chart Onboarded")})
    public ResponseEntity<Void> onboardChart(@RequestPart("chart") MultipartFile chartFile,
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
    @Operation(summary = "Delete the chart")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Chart Deleted")})
    public ResponseEntity<Void> deleteChart(@PathVariable("name") String name,
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
    @ApiResponses(
            value = {@ApiResponse(responseCode = "201", description = "Repository added"),
                @ApiResponse(responseCode = "409", description = "Repository already Exist")})
    public ResponseEntity<String> configureRepo(@RequestBody String repo)
            throws ServiceException, IOException {
        HelmRepository repository;
        try {
            repository = CODER.decode(repo, HelmRepository.class);
        } catch (CoderException e) {
            logger.warn("Error parsing the repository information:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error parsing the repository information");
        }
        if (chartService.configureRepository(repository)) {
            return new ResponseEntity<>(HttpStatus.CREATED);
        }
        return new ResponseEntity<>(HttpStatus.CONFLICT);
    }
}

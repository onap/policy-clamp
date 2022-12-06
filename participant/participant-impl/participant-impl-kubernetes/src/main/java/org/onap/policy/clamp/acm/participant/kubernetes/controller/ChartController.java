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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.kubernetes.controller.genapi.KubernetesParticipantControllerApi;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceRuntimeException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController("chartController")
@ConditionalOnExpression("${chart.api.enabled:false}")
@RequestMapping("helm")
public class ChartController implements KubernetesParticipantControllerApi {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ChartService chartService;

    private static final StandardCoder CODER = new StandardCoder();

    /**
     * REST endpoint to get all the charts.
     *
     * @return List of charts installed
     */
    @Override
    public ResponseEntity<ChartList> getAllCharts(UUID onapRequestId) {
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
    @Override
    public ResponseEntity<Void> installChart(UUID onapRequestId, InstallationInfo info) {
        ChartInfo chart = chartService.getChart(info.getName(), info.getVersion());
        if (chart == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            chartService.installChart(chart);
        } catch (ServiceException | IOException e) {
            throw new ServiceRuntimeException(e);
        }
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
    @Override
    public ResponseEntity<Void> uninstallChart(String name, String version, UUID onapRequestId) {
        ChartInfo chart = chartService.getChart(name, version);
        if (chart == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            chartService.uninstallChart(chart);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (ServiceException se) {
            throw se.asRuntimeException();
        }
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
    @Override
    public ResponseEntity<Void> onboardChart(MultipartFile chartFile, MultipartFile overrideFile, String infoJson,
        UUID onapRequestId) {

        ChartInfo info;
        try {
            info = CODER.decode(infoJson, ChartInfo.class);
        } catch (CoderException e) {
            throw new ServiceRuntimeException("Error parsing the chart information", e);
        }

        try {
            chartService.saveChart(info, chartFile, overrideFile);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException | ServiceException e) {
            throw new ServiceRuntimeException(e);
        }
    }

    /**
     * REST endpoint to delete a specific helm chart.
     *
     * @param name name of the chart
     * @param version version of the chart
     * @return Status of operation
     */
    @Override
    public ResponseEntity<Void> deleteChart(String name, String version, UUID onapRequestId) {

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
    @Override
    public ResponseEntity<String> configureRepo(UUID onapRequestId, String repo) {
        HelmRepository repository;
        try {
            repository = CODER.decode(repo, HelmRepository.class);
        } catch (CoderException e) {
            logger.warn("Error parsing the repository information:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error parsing the repository information");
        }

        try {
            if (chartService.configureRepository(repository)) {
                return new ResponseEntity<>(HttpStatus.CREATED);
            } else {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }
        } catch (ServiceException se) {
            throw se.asRuntimeException();
        }
    }
}

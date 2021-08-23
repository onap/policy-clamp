/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.clds.config.spring;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.onap.policy.clamp.clds.config.ClampProperties;
import org.onap.policy.clamp.clds.config.sdc.SdcControllersConfiguration;
import org.onap.policy.clamp.clds.exception.sdc.controller.SdcControllerException;
import org.onap.policy.clamp.clds.sdc.controller.SdcSingleController;
import org.onap.policy.clamp.clds.sdc.controller.SdcSingleControllerStatus;
import org.onap.policy.clamp.loop.CsarInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Profile("clamp-sdc-controller")
public class SdcControllerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SdcControllerConfiguration.class);
    private List<SdcSingleController> sdcControllersList = new ArrayList<>();
    private final ClampProperties clampProp;
    private final CsarInstaller csarInstaller;

    @Autowired
    public SdcControllerConfiguration(ClampProperties clampProp,
            @Qualifier("csarInstaller") CsarInstaller csarInstaller) {
        this.clampProp = clampProp;
        this.csarInstaller = csarInstaller;
    }

    /**
     * Loads SDC controller configuration.
     */
    @PostConstruct
    public void loadSdcControllers() {
        var sdcControllersConfig = getSdcControllersConfiguration();
        sdcControllersConfig.getAllDefinedControllers().forEach((key, value) -> {
            logger.info("Creating controller instance: {}", key);
            var sdcController = new SdcSingleController(clampProp, csarInstaller, value, null);
            sdcControllersList.add(sdcController);
        });
    }

    /**
     * Checks whether all SDC controllers defined are up and running.
     */
    @Scheduled(fixedRate = 120000)
    public void checkAllSdcControllers() {
        logger.info("Checking that all SDC Controllers defined are up and running");
        for (SdcSingleController controller : sdcControllersList) {
            try {
                if (SdcSingleControllerStatus.STOPPED.equals(controller.getControllerStatus())) {
                    controller.initSdc();
                }
            } catch (SdcControllerException e) {
                logger.error("Exception caught when booting sdc controller", e);
            }
        }
        logger.info("SDC Controllers check completed");
    }

    /**
     * Closes all SDC Controller and the SDC Client.
     */
    @PreDestroy
    public void killSdcControllers() {
        sdcControllersList.forEach(e -> {
            try {
                e.closeSdc();
            } catch (SdcControllerException e1) {
                logger.error("Exception caught when stopping sdc controller", e1);
            }
        });
    }

    @Bean(name = "sdcControllersConfiguration")
    public SdcControllersConfiguration getSdcControllersConfiguration() {
        return new SdcControllersConfiguration();
    }
}

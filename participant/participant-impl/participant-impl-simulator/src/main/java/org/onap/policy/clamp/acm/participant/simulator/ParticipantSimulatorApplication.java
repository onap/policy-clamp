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

package org.onap.policy.clamp.acm.participant.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Starter.
 *
 */
// @formatter:off
@SpringBootApplication
@ConfigurationPropertiesScan("org.onap.policy.clamp.acm.participant.simulator.main.parameters")
@ComponentScan({
    "org.onap.policy.clamp.acm.participant.simulator",
    "org.onap.policy.clamp.acm.participant.intermediary",
    "org.onap.policy.clamp.acm.common.rest"
})
//@formatter:on
public class ParticipantSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParticipantSimulatorApplication.class, args);
    }
}

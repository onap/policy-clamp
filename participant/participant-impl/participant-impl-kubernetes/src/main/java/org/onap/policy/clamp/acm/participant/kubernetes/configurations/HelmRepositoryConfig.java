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

package org.onap.policy.clamp.acm.participant.kubernetes.configurations;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.onap.policy.clamp.acm.participant.kubernetes.models.HelmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "helm")
@Data
public class HelmRepositoryConfig {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private List<HelmRepository> repos = new ArrayList<>();

    private List<String> protocols = new ArrayList<>();
}

/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.config;

import javax.annotation.PostConstruct;
import org.onap.clamp.loop.template.PolicyModel;
import org.onap.clamp.loop.template.PolicyModelsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("legacy-operational-policy")
public class LegacyOperationalPolicy {

    @Autowired
    PolicyModelsService policyModelService;

    public static final String OPERATIONAL_POLICY_LEGACY = "onap.policies.operational.legacy";

    @PostConstruct
    public void init() {
        policyModelService.saveOrUpdatePolicyModel(new PolicyModel(OPERATIONAL_POLICY_LEGACY, "", "1.0.0",
                "OperationalPolicyLegacy"));
    }
}


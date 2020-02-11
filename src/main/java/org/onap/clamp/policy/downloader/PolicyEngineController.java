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

package org.onap.clamp.policy.downloader;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.onap.clamp.clds.client.PolicyEngineServices;
import org.onap.clamp.loop.template.PolicyModelsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.yaml.snakeyaml.Yaml;

/**
 * This class implements a periodic job that is done in the background to
 * synchronize policy models available on the policy engine and the clamp
 * database table PolicyModel.
 */
@Configuration
@Profile("clamp-policy-controller")
public class PolicyEngineController {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(PolicyEngineController.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    public static final String POLICY_RETRY_INTERVAL = "policy.retry.interval";
    public static final String POLICY_RETRY_LIMIT = "policy.retry.limit";

    private final PolicyEngineServices policyEngineServices;

    @Autowired
    public PolicyEngineController(PolicyEngineServices policyEngineService,
            PolicyModelsRepository policyModelsRepository) {
        this.policyEngineServices = policyEngineService;
    }

    @Scheduled(fixedRate = 120000)
    public void synchronizeAllPolicies() {
        LinkedHashMap<String, Object> loadedYaml;
        loadedYaml = new Yaml().load(policyEngineServices.downloadAllPolicies());
        if (loadedYaml == null || loadedYaml.isEmpty()) {
            logger.warn("getAllPolicyType yaml returned by policy engine could not be decoded, as it's null or empty");
            return;
        }

        List<LinkedHashMap<String, Object>> policyTypesList = (List<LinkedHashMap<String, Object>>) loadedYaml
                .get("policy_types");
        policyTypesList.parallelStream().forEach(policyType -> {
            Entry<String, Object> policyTypeEntry = (Entry<String, Object>) new ArrayList(policyType.entrySet()).get(0);

            policyEngineServices.createPolicyInDbIfNeeded(
                    policyEngineServices.createPolicyModelFromPolicyEngine(policyTypeEntry.getKey(),
                            ((String) ((LinkedHashMap<String, Object>) policyTypeEntry.getValue()).get("version"))));

        });
    }

}

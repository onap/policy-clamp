/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.main.utils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.springframework.stereotype.Component;

/**
 * Class to encrypt/decrypt sensitive fields in the database.
 */

@Component
public class EncryptionUtils {

    public EncryptionUtils(AcRuntimeParameterGroup acRuntimeParameterGroup) {
        // it will be removed
    }

    /**
     * Check encryption is enabled.
     * @return boolean result
     */
    public boolean encryptionEnabled() {
        return false;
    }


    /**
     * Find and encrypt sensitive fields in an AC instance.
     * @param acDefinition acDefinition
     * @param automationComposition acInstance
     */
    public void findAndEncryptSensitiveData(AutomationCompositionDefinition acDefinition,
                                            AutomationComposition automationComposition) {
        // it will be removed
    }

    /**
     * Find and decrypt sensitive fields in an AC instance.
     *
     * @param acElements element map
     */
    public void decryptInstanceProperties(Map<UUID, AutomationCompositionElement> acElements) {
        // it will be removed
    }

    /**
     * Find and decrypt sensitive fields in an AC instance list.
     *
     * @param automationCompositionList acInstance list
     */
    public void decryptInstanceProperties(List<AutomationComposition> automationCompositionList) {
        // it will be removed
    }
}

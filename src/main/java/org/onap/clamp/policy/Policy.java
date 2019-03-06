/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
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

package org.onap.clamp.policy;

import com.google.gson.JsonObject;

public interface Policy {

    String getName();

    JsonObject getJsonRepresentation();

    static String generatePolicyName(String policyType, String serviceName, String serviceVersion, String resourceName,
        String blueprintFilename) {
        StringBuilder buffer = new StringBuilder(policyType).append("_").append(serviceName).append("_v")
            .append(serviceVersion).append("_").append(resourceName).append("_")
            .append(blueprintFilename.replaceAll(".yaml", ""));
        return buffer.toString().replace('.', '_').replaceAll(" ", "");
    }

}

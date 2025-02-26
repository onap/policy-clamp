/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023,2025 Nordix Foundation.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.runtime.main.parameters;

import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

/**
 * Parameters for ac element name and  ac node type.
 */
@Getter
@Setter
@Validated
public class AcmParameters {

    // Default values for the element name and composition name
    private String toscaElementName = "org.onap.policy.clamp.acm.AutomationCompositionElement";

    private String toscaCompositionName = "org.onap.policy.clamp.acm.AutomationComposition";

    private boolean enableEncryption = false;

}

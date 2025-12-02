/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2021, 2025 Nordix Foundation.
 *  Modifications Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
 *  Modifications Copyright (C) 2024 Nordix Foundation
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

package org.onap.policy.models.tosca.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;

/**
 * Import the {@link ToscaUtils} class.
 *
 * @author Liam Fallon (liam.fallon@est.tech)
 */
class ToscaUtilsTest {

    @Test
    void testGetPredefinedDataTypes() {
        assertTrue(ToscaUtils.getPredefinedDataTypes().contains(new PfConceptKey("string", PfKey.NULL_KEY_VERSION)));
    }
}

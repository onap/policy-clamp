/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.persistence.concepts;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


class JpaMessageJobTest {

    @Test
    void testJpaMessageJobConstructor() {
        assertThatThrownBy(() -> new JpaMessageJob(null))
                .hasMessageMatching("identificationId is marked .*ull but is null");
    }

    @Test
    void testJpaMessageValidation() {
        var jpaMessageJob = new JpaMessageJob();

        assertThatThrownBy(() -> jpaMessageJob.validate(null))
                .hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(jpaMessageJob.validate("").isValid());

        jpaMessageJob.setJobStarted(null);
        assertFalse(jpaMessageJob.validate("").isValid());
    }
}

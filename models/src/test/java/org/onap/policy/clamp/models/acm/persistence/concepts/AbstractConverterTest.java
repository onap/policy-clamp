/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.base.PfModelRuntimeException;

class AbstractConverterTest {

    static class TestConverter extends AbstractConverter {

    }

    @Test
    void testNull() {
        assertThatThrownBy(() -> AbstractConverter.convertObject("-", Map.class))
                .isInstanceOf(PfModelRuntimeException.class);

        var converter = new TestConverter();
        var dbData = converter.encode(null);
        assertThat(dbData).isNull();

        assertThatThrownBy(() -> converter.decode("-", Map.class))
                .isInstanceOf(PfModelRuntimeException.class);
    }

}

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

package org.onap.clamp.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SemanticVersioningTest {

    /**
     * compare test.
     */
    @Test
    public void compareTest() {
        assertThat(SemanticVersioning.compare("1.0.0", "2.0.0")).isEqualTo(-1);
        assertThat(SemanticVersioning.compare("1.5.0", "2.0.0")).isEqualTo(-1);
        assertThat(SemanticVersioning.compare("1.5.0", "2.1.0")).isEqualTo(-1);
        assertThat(SemanticVersioning.compare("1.5.3", "2.0.0")).isEqualTo(-1);
        assertThat(SemanticVersioning.compare("2.5.3", "2.6.0")).isEqualTo(-1);
        assertThat(SemanticVersioning.compare("2.5", "2.5.1")).isEqualTo(-1);
        assertThat(SemanticVersioning.compare("2.5.0", "2.5.1")).isEqualTo(-1);
        assertThat(SemanticVersioning.compare("2.5.0.0", "2.5.1")).isEqualTo(-1);
        assertThat(SemanticVersioning.compare("2.5.1.0", "2.5.1")).isEqualTo(1);

        assertThat(SemanticVersioning.compare("2.0.0", "1.0.0")).isEqualTo(1);
        assertThat(SemanticVersioning.compare("2.0.0", "1.5.0")).isEqualTo(1);
        assertThat(SemanticVersioning.compare("2.1.0", "1.5.0")).isEqualTo(1);
        assertThat(SemanticVersioning.compare("2.0.0", "1.5.3")).isEqualTo(1);
        assertThat(SemanticVersioning.compare("2.6.0", "2.5.3")).isEqualTo(1);
        assertThat(SemanticVersioning.compare("2.5.1", "2.5")).isEqualTo(1);
        assertThat(SemanticVersioning.compare("2.5.1", "2.5.0")).isEqualTo(1);
        assertThat(SemanticVersioning.compare("2.5.1", "2.5.0.0")).isEqualTo(1);
        assertThat(SemanticVersioning.compare("1", "1.2.3.0")).isEqualTo(-1);
        assertThat(SemanticVersioning.compare("1.2", "1")).isEqualTo(1);
    }

    /**
     * Compare test.
     */
    @Test
    public void compareEqualsTest() {
        assertThat(SemanticVersioning.compare("1.0.0", "1.0.0")).isEqualTo(0);
        assertThat(SemanticVersioning.compare("1.0.0.0", "1.0.0")).isEqualTo(1);
        assertThat(SemanticVersioning.compare("1.2.3", "1.2.3")).isEqualTo(0);
        assertThat(SemanticVersioning.compare("1.2.3", "1.2.3.0")).isEqualTo(-1);

    }

    /**
     * Compare with null.
     */
    @Test
    public void compareNullTest() {
        assertThat(SemanticVersioning.compare(null, null)).isEqualTo(0);
        assertThat(SemanticVersioning.compare(null, "1.0")).isEqualTo(-1);
        assertThat(SemanticVersioning.compare("1.0", null)).isEqualTo(1);
    }

    /**
     * Increment major version test.
     */
    @Test
    public void incrementVersionTest() {
        assertThat(SemanticVersioning.incrementMajorVersion("1.0")).isEqualTo("2.0.0");
        assertThat(SemanticVersioning.incrementMajorVersion("1.0.0")).isEqualTo("2.0.0");
        assertThat(SemanticVersioning.incrementMajorVersion("1")).isEqualTo("2.0.0");
        assertThat(SemanticVersioning.incrementMajorVersion("1.2.3")).isEqualTo("2.0.0");
    }
}

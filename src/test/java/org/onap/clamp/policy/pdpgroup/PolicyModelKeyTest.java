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

package org.onap.clamp.policy.pdpgroup;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.Test;

public class PolicyModelKeyTest {

    @Test
    public void testEqualsMethod() throws IOException {
        PolicyModelKey key1 = new PolicyModelKey("name1","1.0.0");
        PolicyModelKey key2 = new PolicyModelKey(null,"1.0.0");
        PolicyModelKey key3 = new PolicyModelKey("name1",null);

        assertThat(key1.equals(null)).isFalse();
        assertThat(key1.equals("key2")).isFalse();

        assertThat(key2.equals(key1)).isFalse();
        assertThat(key3.equals(key1)).isFalse();

        PolicyModelKey key4 = new PolicyModelKey("name2","1.0.0");
        PolicyModelKey key5 = new PolicyModelKey("name1","2.0.0");
        assertThat(key1.equals(key4)).isFalse();
        assertThat(key1.equals(key5)).isFalse();

        PolicyModelKey key6 = new PolicyModelKey("name(.*)","1.0.0");
        PolicyModelKey key7 = new PolicyModelKey("name1","1.0.0");
        assertThat(key1.equals(key6)).isTrue();
        assertThat(key1.equals(key7)).isTrue();
    }
}

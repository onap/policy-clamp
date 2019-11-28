/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PassDecoderTest {

    private final String encrypted = "enc:WWCxchk4WGBNSvuzLq3MLjMs5ObRybJtts5AI0XD1Vc";

    @Test
    public final void testDecryptionNoKeyfile() throws Exception {
        String decodedPass = PassDecoder.decode(encrypted, null);
        assertEquals(decodedPass, encrypted);
    }

    @Test
    public final void testDecryptionNoPassword() throws Exception {
        String decodedPass = PassDecoder.decode(null, "src/test/resources/clds/aaf/org.onap.clamp.keyfile");
        assertNull(decodedPass);
    }

    @Test
    public final void testDecryption() throws Exception {
        String decodedPass = PassDecoder.decode(encrypted, "src/test/resources/clds/aaf/org.onap.clamp.keyfile");
        assertEquals(decodedPass, "China in the Spring");
    }
}

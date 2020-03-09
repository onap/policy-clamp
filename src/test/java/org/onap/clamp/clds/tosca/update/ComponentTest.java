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

package org.onap.clamp.clds.tosca.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import junit.framework.TestCase;
import org.onap.clamp.clds.util.ResourceFileUtil;

public class ComponentTest extends TestCase {

    /**
     * Test propertiesName.
     *
     * @throws IOException In case of failure
     */
    public void testPropertiesNames() throws IOException {
        ArrayList<String> reference = new ArrayList<>(Arrays.asList("actor", "operation", "target", "payload"));
        TemplateManagement templateManagement =
                new TemplateManagement(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/defaultToscaTypes.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.json"));
        Component component = templateManagement.getComponents().get("onap.datatype.controlloop.Actor");
        assertEquals(reference, component.propertiesNames());
    }

}

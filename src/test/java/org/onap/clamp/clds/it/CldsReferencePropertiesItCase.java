/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.it;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.AbstractItCase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test corg.onap.clamp.ClampDesigner.model.refprop package using RefProp.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class CldsReferencePropertiesItCase extends AbstractItCase {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(CldsReferencePropertiesItCase.class);

    /**
     * Test getting prop value as a JSON Node / template.
     *
     * @throws IOException
     *             when JSON parsing fails
     */
    @Test
    public void testJsonTemplate() throws IOException {
        ObjectNode root = (ObjectNode) refProp.getJsonTemplate("tca.template");
        root.put("closedLoopControlName", "ClosedLoop-FRWL-SIG-1582f840-2881-11e6-b4ec-005056a9d756");
        ObjectMapper mapper = new ObjectMapper();
        String jsonText = mapper.writeValueAsString(root);
        logger.error("Exception caught IllegalArgumentException as expected");
        // assertEquals(topicsJson, ref.getTopicsToJson());
    }
}

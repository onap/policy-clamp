/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.transform;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.skyscreamer.jsonassert.JSONAssert;

public class XslTransformerTest {

    /**
     * This test validates the XSLT to convert BPMN xml to BPMN JSON.
     * 
     * @throws TransformerException
     *             In case of issues
     * @throws IOException
     *             In case of issues
     */
    @Test
    public void xslTransformTest() throws TransformerException, IOException {
        XslTransformer xslTransformer = new XslTransformer();
        xslTransformer.setXslResourceName("xsl/clds-bpmn-transformer.xsl");

        String bpmnJson = xslTransformer
                .doXslTransformToString(ResourceFileUtil.getResourceAsString("example/xsl-validation/modelBpmn.xml"));
        assertNotNull(bpmnJson);
        JSONAssert.assertEquals(ResourceFileUtil.getResourceAsString("example/xsl-validation/modelBpmnForVerif.json"),
                bpmnJson, true);
    }
}

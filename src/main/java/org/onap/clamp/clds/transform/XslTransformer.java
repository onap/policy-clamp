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

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * XSL Transformer.
 */
public class XslTransformer {

    private Templates templates;

    public void setXslResourceName(String xslResourceName) throws TransformerConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        templates = tfactory.newTemplates(new StreamSource(TransformUtil.getResourceAsStream(xslResourceName)));
    }

    /**
     * Given xml input, return the transformed result.
     *
     * @param xml
     * @throws TransformerException
     */
    public String doXslTransformToString(String xml) throws TransformerException {
        StringWriter output = new StringWriter(4000);

        Transformer transformer = templates.newTransformer();
        transformer.transform(new StreamSource(new StringReader(xml)),
                new StreamResult(output));
        return output.toString();
    }

}

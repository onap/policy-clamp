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
 * 
 */

package org.onap.clamp.clds.transform;

import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.onap.clamp.clds.util.ResourceFileUtil;

/**
 * XSL Transformer.
 */
public class XslTransformer {

    private Templates templates;

    public void setXslResourceName(String xslResourceName) throws TransformerConfigurationException {
        TransformerFactory tfactory = new TransformerFactoryImpl();
        tfactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        templates = tfactory.newTemplates(new StreamSource(ResourceFileUtil.getResourceAsStream(xslResourceName)));
    }

    /**
     * Given xml input, return the transformed result.
     *
     * @param xml
     * @throws TransformerException
     */
    public String doXslTransformToString(String xml) throws TransformerException {
        StringWriter output = new StringWriter(4096);

        Transformer transformer = templates.newTransformer();
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(output));
        return output.toString();
    }

}

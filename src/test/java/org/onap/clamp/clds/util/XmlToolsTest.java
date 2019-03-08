/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia. All rights
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

package org.onap.clamp.clds.util;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.util.SVGConstants;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlToolsTest {

    @Test
    public void exportXmlDocumentAsStringTest() throws IOException, ParserConfigurationException, SAXException {
        String expected = ResourceFileUtil.getResourceAsString("clds/util/file.xml");
        Document document = parseStringToXmlDocument(expected);
        String actual = XmlTools.exportXmlDocumentAsString(document);
        Assert.assertEquals(expected.trim(), actual.trim());
    }

    @Test
    public void createEmptySvgDocumentTest() {
        Document doc = XmlTools.createEmptySvgDocument();
        Assert.assertEquals(SVGDOMImplementation.SVG_NAMESPACE_URI, doc.getDocumentElement().getNamespaceURI());
        Assert.assertEquals(SVGConstants.SVG_SVG_TAG, doc.getDocumentElement().getNodeName());
        Assert.assertNull(doc.getDoctype());
    }


    public static Document parseStringToXmlDocument(String res)
        throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://xml.org/sax/features/namespaces", false);
        dbf.setFeature("http://xml.org/sax/features/validation", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(res));
        return db.parse(is);
    }

}
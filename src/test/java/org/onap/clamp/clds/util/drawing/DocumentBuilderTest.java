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

package org.onap.clamp.clds.util.drawing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.util.SVGConstants;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.clamp.clds.util.XmlToolsTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

@RunWith(MockitoJUnitRunner.class)
public class DocumentBuilderTest {
    @Mock
    private SVGGraphics2D mockG2d;

    @Test
    public void pushChangestoDocumentTest() throws IOException, ParserConfigurationException, SAXException {
        String dataElementId = "someId";
        String newNodeTag = "tagged";
        String newNodeText = "Sample text";
        String xml = ResourceFileUtil.getResourceAsString("clds/util/file.xml");
        Document document = XmlToolsTest.parseStringToXmlDocument(xml);
        Node newNode = document.createElement(newNodeTag);
        newNode.appendChild(document.createTextNode(newNodeText));

        when(mockG2d.getRoot(any(Element.class))).then(a -> a.getArgument(0, Element.class).appendChild(newNode));

        DocumentBuilder db = new DocumentBuilder(document, document);
        db.pushChangestoDocument(mockG2d, dataElementId);
        Document actualDocument = db.getGroupingDocument();

        Node addedActualNode = actualDocument.getDocumentElement().getLastChild();
        String actualDataElementId = addedActualNode.getAttributes()
                .getNamedItem(DocumentBuilder.DATA_ELEMENT_ID_ATTRIBUTE).getTextContent();

        Assert.assertEquals(dataElementId, actualDataElementId);
        Assert.assertEquals(SVGConstants.SVG_G_TAG, addedActualNode.getNodeName());

        Node addedActualNodeChild = addedActualNode.getLastChild();
        Assert.assertEquals(newNodeTag, addedActualNodeChild.getNodeName());
        Assert.assertEquals(newNodeText, addedActualNodeChild.getTextContent());
    }
}
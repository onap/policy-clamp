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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.clamp.clds.util.XmlToolsTest;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@RunWith(MockitoJUnitRunner.class)
public class ClampGraphTest {
    @Mock
    private DocumentBuilder mockDocumentBuilder;

    @Test
    public void getAsSvgTest() throws IOException, ParserConfigurationException, SAXException {
        String expected = ResourceFileUtil.getResourceAsString("clds/util/file.xml");
        Document document = XmlToolsTest.parseStringToXmlDocument(expected);

        when(mockDocumentBuilder.getGroupingDocument()).thenReturn(document);

        String actual = new ClampGraph(mockDocumentBuilder).getAsSVG();
        Assert.assertEquals(expected.trim(), actual.trim());
    }

    @Test
    public void getAsSvgLazyTest() throws IOException, ParserConfigurationException, SAXException {
        String expected = ResourceFileUtil.getResourceAsString("clds/util/file.xml");
        Document document = XmlToolsTest.parseStringToXmlDocument(expected);

        when(mockDocumentBuilder.getGroupingDocument()).thenReturn(document);
        ClampGraph cg = new ClampGraph(mockDocumentBuilder);

        String actualFirst = cg.getAsSVG();
        verify(mockDocumentBuilder, times(1)).getGroupingDocument();

        String actualSecond = cg.getAsSVG();
        verifyNoMoreInteractions(mockDocumentBuilder);

        Assert.assertEquals(expected.trim(), actualFirst.trim());
        Assert.assertEquals(expected.trim(), actualSecond.trim());

    }
}

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

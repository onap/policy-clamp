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
 * 
 */

package org.onap.clamp.clds.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import javax.ws.rs.core.SecurityContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.model.ValueItem;
import org.onap.clamp.clds.service.CldsTemplateService;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test HTTP and HTTPS settings + redirection of HTTP to HTTPS.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class CldsTemplateServiceItCase {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(CldsTemplateServiceItCase.class);
    @Autowired
    private CldsTemplateService cldsTemplateService;
    @Autowired
    private CldsDao cldsDao;
    private String bpmnText;
    private String imageText;
    private String bpmnPropText;
    private CldsTemplate cldsTemplate;

    /**
     * Setup the variable before the tests execution.
     * 
     * @throws IOException
     *             In case of issues when opening the files
     */
    @Before
    public void setupBefore() throws IOException {
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn("admin");
        Mockito.when(securityContext.getUserPrincipal()).thenReturn(principal);
        Mockito.when(securityContext.isUserInRole("permission-type-cl|dev|read")).thenReturn(true);
        Mockito.when(securityContext.isUserInRole("permission-type-cl|dev|update")).thenReturn(true);
        Mockito.when(securityContext.isUserInRole("permission-type-template|dev|read")).thenReturn(true);
        Mockito.when(securityContext.isUserInRole("permission-type-template|dev|update")).thenReturn(true);
        cldsTemplateService.setSecurityContext(securityContext);
        bpmnText = ResourceFileUtil.getResourceAsString("example/dao/bpmn-template.xml");
        imageText = ResourceFileUtil.getResourceAsString("example/dao/image-template.xml");
        bpmnPropText = ResourceFileUtil.getResourceAsString("example/dao/bpmn-prop.json");
        cldsTemplate = new CldsTemplate();
        cldsTemplate.setName("testModel");
        cldsTemplate.setBpmnText(bpmnText);
        cldsTemplate.setImageText(imageText);
        cldsTemplate.setPropText(bpmnPropText);
        cldsTemplateService.putTemplate("testModel", cldsTemplate);
    }

    @Test
    public void testPutTemplate() throws Exception {
        CldsTemplate savedTemplate = CldsTemplate.retrieve(cldsDao, "testModel", false);
        assertNotNull(savedTemplate);
        logger.info("saved template bpmn text is:" + savedTemplate.getBpmnText());
        assertEquals(bpmnText, savedTemplate.getBpmnText());
        assertEquals(imageText, savedTemplate.getImageText());
        assertEquals(bpmnPropText, savedTemplate.getPropText());
        assertEquals("testModel", savedTemplate.getName());
    }

    @Test
    public void testGetTemplate() throws Exception {
        CldsTemplate getTemplate = cldsTemplateService.getTemplate("testModel");
        assertNotNull(getTemplate);
        assertEquals(bpmnText, getTemplate.getBpmnText());
        assertEquals(imageText, getTemplate.getImageText());
        assertEquals(bpmnPropText, getTemplate.getPropText());
        assertEquals("testModel", getTemplate.getName());
    }

    @Test
    public void testGetImageXml() throws Exception {
        String imageXml = cldsTemplateService.getImageXml("testModel");
        assertEquals(imageText, imageXml);
    }

    @Test
    public void testGetBpmnTemplate() throws Exception {
        String bpmnTemplate = cldsTemplateService.getBpmnTemplate("testModel");
        assertEquals(bpmnText, bpmnTemplate);
    }

    @Test
    public void testGetTemplateNames() throws Exception {
        CldsTemplate cldsTemplateNew = new CldsTemplate();
        cldsTemplateNew.setName("testModelNew");
        cldsTemplateNew.setBpmnText(bpmnText);
        cldsTemplateNew.setImageText(imageText);
        cldsTemplateNew.setPropText(bpmnPropText);
        cldsTemplateService.putTemplate("testModelNew", cldsTemplateNew);
        List<ValueItem> templateNames = cldsTemplateService.getTemplateNames();
        boolean testModel = false;
        boolean testModelNew = false;
        for (ValueItem item : templateNames) {
            if (item.getValue().equals("testModel")) {
                testModel = true;
            }
            if (item.getValue().equals("testModelNew")) {
                testModelNew = true;
            }
        }
        assertTrue(testModel || testModelNew);
    }
}

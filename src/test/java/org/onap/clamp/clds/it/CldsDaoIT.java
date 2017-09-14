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

package org.onap.clamp.clds.it;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ws.rs.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.AbstractIT;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test CldsDAO calls through CldsModel and CldsEvent. This really test the DB
 * and stored procedures.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:application-no-camunda.properties")
public class CldsDaoIT extends AbstractIT {

    @Autowired
    public CldsDao cldsDao;

    private String bpmnText;
    private String imageText;
    private String bpmnPropText;

    @Before
    public void setupBefore() throws IOException {
        bpmnText = ResourceFileUtil.getResourceAsString("example/dao/bpmn-template.xml");
        imageText = ResourceFileUtil.getResourceAsString("example/dao/image-template.xml");
        bpmnPropText = ResourceFileUtil.getResourceAsString("example/dao/bpmn-prop.json");
    }

    @Test
    public void testModelSave() throws IOException {
        // Add the template first
        CldsTemplate newTemplate = new CldsTemplate();
        newTemplate.setName("test-template");

        newTemplate.setBpmnText(bpmnText);
        newTemplate.setImageText(imageText);

        // Save the template in DB
        cldsDao.setTemplate(newTemplate, "user");
        // Test if it's well there
        CldsTemplate newTemplateRead = cldsDao.getTemplate("test-template");
        assertEquals(bpmnText, newTemplateRead.getBpmnText());
        assertEquals(imageText, newTemplateRead.getImageText());

        // Save the model
        CldsModel newModel = new CldsModel();
        newModel.setName("test-model");

        newModel.setBpmnText(bpmnText);
        newModel.setImageText(imageText);
        newModel.setPropText(bpmnPropText);
        newModel.setControlNamePrefix("ClosedLoop-");
        newModel.setTemplateName("test-template");
        newModel.setTemplateId(newTemplate.getId());
        newModel.setDocText(newTemplate.getPropText());
        newModel.setDocId(newTemplate.getPropId());
        // Save the model in DB
        cldsDao.setModel(newModel, "user");
        // Test if the model can be retrieved
        CldsModel newCldsModel = cldsDao.getModelTemplate("test-model");
        assertEquals(bpmnText, newCldsModel.getBpmnText());
        assertEquals(imageText, newCldsModel.getImageText());
        assertEquals(bpmnPropText, newCldsModel.getPropText());

    }

    @Test(expected = NotFoundException.class)
    public void testGetModelNotFound() {
        CldsModel.retrieve(cldsDao, "test-model-not-found", false);
    }

    @Test(expected = NotFoundException.class)
    public void testGetTemplateNotFound() {
        CldsTemplate.retrieve(cldsDao, "test-template-not-found", false);
    }

    @Test
    public void testInsEvent() {
        // Add the template first
        CldsTemplate newTemplate = new CldsTemplate();
        newTemplate.setName("test-template-for-event");

        newTemplate.setBpmnText(bpmnText);
        newTemplate.setImageText(imageText);

        newTemplate.save(cldsDao, "user");

        // Test if it's well there
        CldsTemplate newTemplateRead = CldsTemplate.retrieve(cldsDao, "test-template-for-event", false);
        assertEquals(bpmnText, newTemplateRead.getBpmnText());
        assertEquals(imageText, newTemplateRead.getImageText());

        // Save the model
        CldsModel newModel = new CldsModel();
        newModel.setName("test-model-for-event");

        newModel.setBpmnText(bpmnText);
        newModel.setImageText(imageText);
        newModel.setPropText(bpmnPropText);
        newModel.setControlNamePrefix("ClosedLoop-");
        newModel.setTemplateName("test-template-for-event");
        newModel.setTemplateId(newTemplate.getId());
        newModel.setDocText(newTemplate.getPropText());
        newModel.setDocId(newTemplate.getPropId());

        CldsEvent.insEvent(cldsDao, newModel, "user", CldsEvent.ACTION_RESTART, CldsEvent.ACTION_STATE_COMPLETED,
                "process-instance-id");
    }
}

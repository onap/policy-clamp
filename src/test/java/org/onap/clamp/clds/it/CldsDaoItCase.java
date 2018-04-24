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
import static org.junit.Assert.assertNull;

import com.att.aft.dme2.internal.apache.commons.lang.RandomStringUtils;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.NotFoundException;

import org.apache.commons.codec.DecoderException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.client.req.sdc.SdcCatalogServices;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsDbServiceCache;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.CldsMonitoringDetails;
import org.onap.clamp.clds.model.CldsServiceData;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test CldsDAO calls through CldsModel and CldsEvent. This really test the DB
 * and stored procedures.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class CldsDaoItCase {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(CldsDao.class);
    @Autowired
    public CldsDao cldsDao;
    private String bpmnText;
    private String imageText;
    private String bpmnPropText;
    @Autowired
    private SdcCatalogServices sdcCatalogServices;

    /**
     * Setup the variable before the tests execution.
     * 
     * @throws IOException
     *             In case of issues when opening the files
     */
    @Before
    public void setupBefore() throws IOException {
        bpmnText = ResourceFileUtil.getResourceAsString("example/dao/bpmn-template.xml");
        imageText = ResourceFileUtil.getResourceAsString("example/dao/image-template.xml");
        bpmnPropText = ResourceFileUtil.getResourceAsString("example/dao/bpmn-prop.json");
    }

    @Test
    public void testModelSave() {
        String randomNameTemplate = RandomStringUtils.randomAlphanumeric(5);
        // Add the template first
        CldsTemplate newTemplate = new CldsTemplate();
        newTemplate.setName(randomNameTemplate);
        newTemplate.setBpmnText(bpmnText);
        newTemplate.setImageText(imageText);
        // Save the template in DB
        cldsDao.setTemplate(newTemplate, "user");
        // Test if it's well there
        CldsTemplate newTemplateRead = cldsDao.getTemplate(randomNameTemplate);
        assertEquals(bpmnText, newTemplateRead.getBpmnText());
        assertEquals(imageText, newTemplateRead.getImageText());
        // Save the model
        CldsModel newModel = new CldsModel();
        String randomNameModel = RandomStringUtils.randomAlphanumeric(5);
        newModel.setName(randomNameModel);
        newModel.setBpmnText(bpmnText);
        newModel.setImageText(imageText);
        newModel.setPropText(bpmnPropText);
        newModel.setControlNamePrefix("ClosedLoop-");
        newModel.setTemplateName(randomNameTemplate);
        newModel.setTemplateId(newTemplate.getId());
        newModel.setDocText(newTemplate.getPropText());
        // Save the model in DB
        cldsDao.setModel(newModel, "user");
        // Test if the model can be retrieved
        CldsModel newCldsModel = cldsDao.getModelTemplate(randomNameModel);
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
        CldsEvent.insEvent(cldsDao, newModel, "user", CldsEvent.ACTION_RESTART, CldsEvent.ACTION_STATE_COMPLETED,
                "process-instance-id");
    }

    @Test
    public void testGetCldsMonitoringDetails() {
        List<CldsMonitoringDetails> cldsMonitoringDetailsList = new ArrayList<CldsMonitoringDetails>();
        cldsMonitoringDetailsList = cldsDao.getCLDSMonitoringDetails();
        cldsMonitoringDetailsList.forEach(clName -> {
            logger.info(clName.getCloseloopName());
            assertNotNull(clName.getCloseloopName());
        });
    }

    @Test
    public void testCldsServiceCache() throws GeneralSecurityException, DecoderException, IOException {
        CldsServiceData cldsServiceData = sdcCatalogServices
                .getCldsServiceDataWithAlarmConditions("4cc5b45a-1f63-4194-8100-cd8e14248c92");
        // Test not in cache so should be null
        CldsServiceData cldsServiceDataCache = cldsDao.getCldsServiceCache("4cc5b45a-1f63-4194-8100-cd8e14248c92");
        assertNull(cldsServiceDataCache);
        cldsDao.setCldsServiceCache(new CldsDbServiceCache(cldsServiceData));
        cldsServiceDataCache = cldsDao.getCldsServiceCache("4cc5b45a-1f63-4194-8100-cd8e14248c92");
        assertNotNull(cldsServiceDataCache);
        assertEquals("56441b4b-0467-41dc-9a0e-e68613838219", cldsServiceDataCache.getServiceUUID());
        assertEquals("4cc5b45a-1f63-4194-8100-cd8e14248c92", cldsServiceDataCache.getServiceInvariantUUID());
        assertEquals(2, cldsServiceDataCache.getCldsVfs().size());
        assertNotNull(cldsServiceDataCache.getAgeOfRecord());
        assertEquals(4, cldsServiceDataCache.getCldsVfs().get(0).getCldsVfcs().size());
        assertEquals("07e266fc-49ab-4cd7-8378-ca4676f1b9ec",
                cldsServiceDataCache.getCldsVfs().get(0).getVfInvariantResourceUUID());
        assertEquals(0, cldsServiceDataCache.getCldsVfs().get(0).getCldsKPIList().size());
        // Second update
        cldsServiceData.setCldsVfs(null);
        cldsDao.setCldsServiceCache(new CldsDbServiceCache(cldsServiceData));
        cldsServiceDataCache = cldsDao.getCldsServiceCache("4cc5b45a-1f63-4194-8100-cd8e14248c92");
        assertNotNull(cldsServiceDataCache);
        assertNull(cldsServiceDataCache.getCldsVfs());
        cldsDao.clearServiceCache();
    }
}

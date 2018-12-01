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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.xml.transform.TransformerException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONException;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsInfo;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.CldsMonitoringDetails;
import org.onap.clamp.clds.model.CldsServiceData;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.model.DcaeEvent;
import org.onap.clamp.clds.service.CldsService;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test HTTP and HTTPS settings + redirection of HTTP to HTTPS.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class CldsServiceItCase {

    @Autowired
    private CldsService cldsService;
    private String bpmnText;
    private String imageText;
    private String bpmnPropText;
    private String docText;

    @Autowired
    private CldsDao cldsDao;
    private Authentication authentication;
    private List<GrantedAuthority> authList = new LinkedList<GrantedAuthority>();
    private LoggingUtils util;

    /**
     * Setup the variable before the tests execution.
     *
     * @throws IOException
     *         In case of issues when opening the files
     */
    @Before
    public void setupBefore() throws IOException {
        bpmnText = ResourceFileUtil.getResourceAsString("example/model-properties/tca_new/tca-template.xml");
        imageText = ResourceFileUtil.getResourceAsString("example/model-properties/tca_new/tca-img.xml");
        bpmnPropText = ResourceFileUtil.getResourceAsString("example/model-properties/tca_new/model-properties.json");
        docText = ResourceFileUtil.getResourceAsString("example/model-properties/tca_new/doc-text.yaml");

        authList.add(new SimpleGrantedAuthority("permission-type-cl-manage|dev|*"));
        authList.add(new SimpleGrantedAuthority("permission-type-cl|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-cl|dev|update"));
        authList.add(new SimpleGrantedAuthority("permission-type-template|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-template|dev|update"));
        authList.add(new SimpleGrantedAuthority("permission-type-filter-vf|dev|*"));
        authList.add(new SimpleGrantedAuthority("permission-type-cl-event|dev|*"));
        authentication = new UsernamePasswordAuthenticationToken(new User("admin", "", authList), "", authList);

        util = Mockito.mock(LoggingUtils.class);
        Mockito.doNothing().when(util).entering(Matchers.any(HttpServletRequest.class), Matchers.any(String.class));
        cldsService.setLoggingUtil(util);

    }

    @Test
    public void testCldsInfoNotAuthorized() {
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Authentication localAuth = Mockito.mock(Authentication.class);
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        Mockito.when(userDetails.getUsername()).thenReturn("admin");
        Mockito.when(securityContext.getAuthentication()).thenReturn(localAuth);
        Mockito.when(localAuth.getPrincipal()).thenReturn(userDetails);

        cldsService.setSecurityContext(securityContext);
        CldsInfo cldsInfo = cldsService.getCldsInfo();
        assertFalse(cldsInfo.isPermissionReadCl());
        assertFalse(cldsInfo.isPermissionReadTemplate());
        assertFalse(cldsInfo.isPermissionUpdateCl());
        assertFalse(cldsInfo.isPermissionUpdateTemplate());
    }

    @Test
    public void testCldsInfoAuthorized() throws Exception {
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

        cldsService.setSecurityContext(securityContext);
        CldsInfo cldsInfo = cldsService.getCldsInfo();
        assertTrue(cldsInfo.isPermissionReadCl());
        assertTrue(cldsInfo.isPermissionReadTemplate());
        assertTrue(cldsInfo.isPermissionUpdateCl());
        assertTrue(cldsInfo.isPermissionUpdateTemplate());
        Properties prop = new Properties();
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("clds-version.properties");
        prop.load(in);
        in.close();
        assertEquals(cldsInfo.getCldsVersion(), prop.getProperty("clds.version"));
        assertEquals(cldsInfo.getUserName(), "admin");
    }

    @Test
    public void testGetCLDSDetails() throws IOException {
        List<CldsMonitoringDetails> cldsMonitoringDetailsList = cldsService.getCLDSDetails();
        assertNotNull(cldsMonitoringDetailsList);
    }

    @Test
    public void testCompleteFlow() throws TransformerException, ParseException {
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

        cldsService.setSecurityContext(securityContext);
        // Add the template first
        CldsTemplate newTemplate = new CldsTemplate();
        String randomNameTemplate = RandomStringUtils.randomAlphanumeric(5);
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
        String randomNameModel = RandomStringUtils.randomAlphanumeric(5);
        CldsModel newModel = new CldsModel();
        newModel.setName(randomNameModel);
        newModel.setBpmnText(bpmnText);
        newModel.setImageText(imageText);
        newModel.setPropText(bpmnPropText);
        newModel.setControlNamePrefix("ClosedLoop-");
        newModel.setTemplateName(randomNameTemplate);
        newModel.setTemplateId(newTemplate.getId());
        newModel.setDocText(docText);
        // Test the PutModel method

        cldsService.putModel(randomNameModel, newModel);

        assertEquals(bpmnText, cldsService.getBpmnXml(randomNameModel));
        assertEquals(imageText, cldsService.getImageXml(randomNameModel));

        // Verify whether it has been added properly or not
        assertNotNull(cldsDao.getModel(randomNameModel));

        CldsModel model = cldsService.getModel(randomNameModel);
        // Verify with GetModel
        assertEquals(model.getTemplateName(), randomNameTemplate);
        assertEquals(model.getName(), randomNameModel);

        assertTrue(cldsService.getModelNames().size() >= 1);

        // Should fail
        ResponseEntity<?> responseEntity = cldsService.putModelAndProcessAction(CldsEvent.ACTION_SUBMIT,
            randomNameModel, "false", cldsService.getModel(randomNameModel));
        assertTrue(responseEntity.getStatusCode().equals(HttpStatus.OK));
        assertNotNull(responseEntity.getBody());
        assertTrue(CldsModel.STATUS_DISTRIBUTED.equals(((CldsModel) responseEntity.getBody()).getStatus()));
        assertTrue(CldsModel.STATUS_DISTRIBUTED.equals(cldsService.getModel(randomNameModel).getStatus()));

        responseEntity = cldsService.deployModel(randomNameModel, cldsService.getModel(randomNameModel));
        assertNotNull(responseEntity);
        assertTrue(responseEntity.getStatusCode().equals(HttpStatus.OK));
        assertNotNull(responseEntity.getBody());
        assertTrue(CldsModel.STATUS_ACTIVE.equals(((CldsModel) responseEntity.getBody()).getStatus()));
        assertTrue(CldsModel.STATUS_ACTIVE.equals(cldsService.getModel(randomNameModel).getStatus()));

        responseEntity = cldsService.putModelAndProcessAction(CldsEvent.ACTION_STOP, randomNameModel, "false",
            cldsService.getModel(randomNameModel));
        assertTrue(responseEntity.getStatusCode().equals(HttpStatus.OK));
        assertNotNull(responseEntity.getBody());
        assertTrue(CldsModel.STATUS_STOPPED.equals(((CldsModel) responseEntity.getBody()).getStatus()));
        assertTrue(CldsModel.STATUS_STOPPED.equals(cldsService.getModel(randomNameModel).getStatus()));

        responseEntity = cldsService.putModelAndProcessAction(CldsEvent.ACTION_RESTART, randomNameModel, "false",
            cldsService.getModel(randomNameModel));
        assertTrue(responseEntity.getStatusCode().equals(HttpStatus.OK));
        assertNotNull(responseEntity.getBody());
        assertTrue(CldsModel.STATUS_ACTIVE.equals(((CldsModel) responseEntity.getBody()).getStatus()));
        assertTrue(CldsModel.STATUS_ACTIVE.equals(cldsService.getModel(randomNameModel).getStatus()));

        responseEntity = cldsService.putModelAndProcessAction(CldsEvent.ACTION_UPDATE, randomNameModel, "false",
            cldsService.getModel(randomNameModel));
        assertTrue(responseEntity.getStatusCode().equals(HttpStatus.OK));
        assertNotNull(responseEntity.getBody());
        assertTrue(CldsModel.STATUS_ACTIVE.equals(((CldsModel) responseEntity.getBody()).getStatus()));
        assertTrue(CldsModel.STATUS_ACTIVE.equals(cldsService.getModel(randomNameModel).getStatus()));

        responseEntity = cldsService.unDeployModel(randomNameModel, cldsService.getModel(randomNameModel));
        assertNotNull(responseEntity);
        assertTrue(responseEntity.getStatusCode().equals(HttpStatus.OK));
        assertNotNull(responseEntity.getBody());
        assertTrue(CldsModel.STATUS_DISTRIBUTED.equals(((CldsModel) responseEntity.getBody()).getStatus()));
        assertTrue(CldsModel.STATUS_DISTRIBUTED.equals(cldsService.getModel(randomNameModel).getStatus()));

        responseEntity = cldsService.putModelAndProcessAction(CldsEvent.ACTION_DELETE, randomNameModel, "false",
            cldsService.getModel(randomNameModel));
        assertNotNull(responseEntity);
        assertTrue(responseEntity.getStatusCode().equals(HttpStatus.OK));
        assertNotNull(responseEntity.getBody());
        try {
            cldsService.getModel(randomNameModel);
            fail("Should have raised an NotFoundException exception");
        } catch (NotFoundException ne) {

        }

    }

    @Test
    public void testDcaePost() {
        DcaeEvent dcaeEvent = new DcaeEvent();
        dcaeEvent.setArtifactName("ClosedLoop_with-enough-characters_TestArtifact.yml");
        dcaeEvent.setEvent(DcaeEvent.EVENT_CREATED);
        dcaeEvent.setResourceUUID("1");
        dcaeEvent.setServiceUUID("2");
        assertEquals(cldsService.postDcaeEvent("false", dcaeEvent),
            "event=created serviceUUID=2 resourceUUID=1 artifactName=ClosedLoop_with-enough-characters_TestArtifact.yml instance count=0 isTest=false");
    }

    @Test
    public void testGetSdcProperties() throws IOException {
        JSONAssert.assertEquals(
            ResourceFileUtil.getResourceAsString("example/sdc/expected-result/sdc-properties-global.json"),
            cldsService.getSdcProperties(), true);
    }

    @Test
    public void testGetSdcServices() throws GeneralSecurityException, DecoderException, JSONException, IOException {
        String result = cldsService.getSdcServices();
        JSONAssert.assertEquals(
            ResourceFileUtil.getResourceAsString("example/sdc/expected-result/all-sdc-services.json"), result, true);
    }

    @Test
    public void testGetSdcPropertiesByServiceUuidForRefresh()
        throws GeneralSecurityException, DecoderException, JSONException, IOException {
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

        cldsService.setSecurityContext(securityContext);
        // Test basic functionalities
        String result = cldsService.getSdcPropertiesByServiceUUIDForRefresh("4cc5b45a-1f63-4194-8100-cd8e14248c92",
            false);
        JSONAssert.assertEquals(
            ResourceFileUtil.getResourceAsString("example/sdc/expected-result/sdc-properties-4cc5b45a.json"), result,
            true);
        // Now test the Cache effect
        CldsServiceData cldsServiceDataCache = cldsDao.getCldsServiceCache("c95b0e7c-c1f0-4287-9928-7964c5377a46");
        // Should not be there, so should be null
        assertNull(cldsServiceDataCache);
        cldsService.getSdcPropertiesByServiceUUIDForRefresh("c95b0e7c-c1f0-4287-9928-7964c5377a46", true);
        // Should be there now, so should NOT be null
        cldsServiceDataCache = cldsDao.getCldsServiceCache("c95b0e7c-c1f0-4287-9928-7964c5377a46");
        assertNotNull(cldsServiceDataCache);
        cldsDao.clearServiceCache();
    }
}

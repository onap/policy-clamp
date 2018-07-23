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

import com.att.aft.dme2.internal.apache.commons.lang.RandomStringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.codec.DecoderException;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsHealthCheck;
import org.onap.clamp.clds.model.CldsInfo;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.CldsServiceData;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.service.CldsService;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @Autowired
    private CldsDao cldsDao;
    private Authentication authentication;
    private List<GrantedAuthority> authList =  new LinkedList<GrantedAuthority>();

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

        authList.add(new SimpleGrantedAuthority("permission-type-cl|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-cl|dev|update"));
        authList.add(new SimpleGrantedAuthority("permission-type-template|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-template|dev|update"));
        authList.add(new SimpleGrantedAuthority("permission-type-filter-vf|dev|*"));
        authentication =  new UsernamePasswordAuthenticationToken(new User("admin", "", authList), "", authList);
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
    public void testPutModel() {
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
        CldsModel newModel = new CldsModel();
        newModel.setName(randomNameTemplate);
        newModel.setBpmnText(bpmnText);
        newModel.setImageText(imageText);
        newModel.setPropText(bpmnPropText);
        newModel.setControlNamePrefix("ClosedLoop-");
        newModel.setTemplateName("test-template");
        newModel.setTemplateId(newTemplate.getId());
        newModel.setDocText(newTemplate.getPropText());
        // Test the PutModel method
        String randomNameModel = RandomStringUtils.randomAlphanumeric(5);
        cldsService.putModel(randomNameModel, newModel);
        // Verify whether it has been added properly or not
        assertNotNull(cldsDao.getModel(randomNameModel));
    }

    @Test
    public void testGetSdcServices() throws GeneralSecurityException, DecoderException, JSONException, IOException {
        String result = cldsService.getSdcServices();
        JSONAssert.assertEquals(
                ResourceFileUtil.getResourceAsString("example/sdc/expected-result/all-sdc-services.json"), result,
                true);
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
                ResourceFileUtil.getResourceAsString("example/sdc/expected-result/sdc-properties-4cc5b45a.json"),
                result, true);
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

/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsToscaModel;
import org.onap.clamp.clds.service.CldsToscaService;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 * Test CLDS Tosca Service APIs.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class CldsToscaServiceItCase {
    
    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(CldsToscaServiceItCase.class);
    @Autowired
    private CldsToscaService cldsToscaService;
    @Autowired
    private CldsDao cldsDao;
    private String toscaModelYaml;
    private Authentication authentication;
    private CldsToscaModel cldsToscaModel;
    private List<GrantedAuthority> authList =  new LinkedList<GrantedAuthority>();
    private LoggingUtils util;
    
    /**
     * Setup the variable before the tests execution.
     * 
     * @throws IOException
     *             In case of issues when opening the files
     */
    @Before
    public void setupBefore() throws IOException {
        authList.add(new SimpleGrantedAuthority("permission-type-cl|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-cl|dev|update"));
        authList.add(new SimpleGrantedAuthority("permission-type-template|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-template|dev|update"));
        authList.add(new SimpleGrantedAuthority("permission-type-filter-vf|dev|*"));
        authList.add(new SimpleGrantedAuthority("permission-type-tosca|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-tosca|dev|update"));
        authentication =  new UsernamePasswordAuthenticationToken(new User("admin", "", authList), "", authList);

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

        util = Mockito.mock(LoggingUtils.class);
        Mockito.doNothing().when(util).entering(Matchers.any(HttpServletRequest.class), Matchers.any(String.class));
        cldsToscaService.setLoggingUtil(util);

        cldsToscaService.setSecurityContext(securityContext);
        
        toscaModelYaml = ResourceFileUtil.getResourceAsString("tosca/tca-policy-test.yaml");
        
        cldsToscaModel = new CldsToscaModel();
        cldsToscaModel.setToscaModelName("tca-policy-test");
        cldsToscaModel.setToscaModelYaml(toscaModelYaml);
        cldsToscaModel.setUserId("admin");
        cldsToscaModel.setPolicyType("tca");
        cldsToscaService.parseToscaModelAndSave("tca-policy-test", cldsToscaModel);
        logger.info("Initial Tosca Model uploaded in DB:" + cldsToscaModel);
    }
    
    @Test
    public void testParseToscaModelAndSave() throws Exception {
        ResponseEntity responseEntity = cldsToscaService.parseToscaModelAndSave("tca-policy-test", cldsToscaModel);
        CldsToscaModel savedModel = (CldsToscaModel) responseEntity.getBody();
        assertNotNull(savedModel);
        logger.info("Parsed Tosca Model is:" + savedModel);
        assertEquals("tca-policy-test", savedModel.getToscaModelName());
    }

    @Test
    public void testGetToscaModel() throws Exception {
        ResponseEntity<CldsToscaModel> responseEntity = cldsToscaService.getToscaModel("tca-policy-test");
        CldsToscaModel savedModel = responseEntity.getBody();
        assertNotNull(savedModel);
        assertEquals("tca-policy-test", savedModel.getToscaModelName());
    }
    
    @Test
    public void testGetToscaModelsByPolicyType() throws Exception {
        ResponseEntity<CldsToscaModel> responseEntity = cldsToscaService.getToscaModelsByPolicyType("tca");
        CldsToscaModel savedModel = responseEntity.getBody();
        assertNotNull(savedModel);
        assertEquals("tca-policy-test", savedModel.getToscaModelName());
        assertEquals("tca", savedModel.getPolicyType());
    }
    
}

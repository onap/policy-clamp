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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.security.Principal;
import java.util.Properties;

import javax.ws.rs.core.SecurityContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.clamp.clds.AbstractIT;
import org.onap.clamp.clds.model.CldsInfo;
import org.onap.clamp.clds.service.CldsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test HTTP and HTTPS settings + redirection of HTTP to HTTPS.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-no-camunda.properties")
public class CldsServiceIT extends AbstractIT {

    @Autowired
    CldsService cldsService;

    @Test
    public void testCldsInfoNotAuthorized() throws Exception {
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Principal p = Mockito.mock(Principal.class);
        Mockito.when(p.getName()).thenReturn("admin");

        Mockito.when(securityContext.getUserPrincipal()).thenReturn(p);
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
        Principal p = Mockito.mock(Principal.class);
        Mockito.when(p.getName()).thenReturn("admin");

        Mockito.when(securityContext.getUserPrincipal()).thenReturn(p);
        Mockito.when(securityContext.isUserInRole("permission-type-cl|dev|read")).thenReturn(true);
        Mockito.when(securityContext.isUserInRole("permission-type-cl|dev|update")).thenReturn(true);
        Mockito.when(securityContext.isUserInRole("permission-type-template|dev|read")).thenReturn(true);
        Mockito.when(securityContext.isUserInRole("permission-type-template|dev|update")).thenReturn(true);

        cldsService.setSecurityContext(securityContext);

        CldsInfo cldsInfo = cldsService.getCldsInfo();
        assertTrue(cldsInfo.isPermissionReadCl());
        assertTrue(cldsInfo.isPermissionReadTemplate());
        assertTrue(cldsInfo.isPermissionUpdateCl());
        assertTrue(cldsInfo.isPermissionUpdateTemplate());

        Properties prop = new Properties();
        // InputStream in =
        // CldsInfo.class.getResourceAsStream("clds-version.properties");
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("clds-version.properties");
        prop.load(in);
        in.close();

        assertEquals(cldsInfo.getCldsVersion(), prop.getProperty("clds.version"));
        assertEquals(cldsInfo.getUserName(), "admin");
    }
}

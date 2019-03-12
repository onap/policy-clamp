/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.clamp.authorization.AuthorizationController;
import org.onap.clamp.clds.service.SecureServicePermission;
import org.onap.clamp.util.PrincipalUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test CldsDAO calls through CldsModel and CldsEvent. This really test the DB
 * and stored procedures.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class AuthorizationControllerItCase {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(AuthorizationControllerItCase.class);
    private Authentication authentication;
    private List<GrantedAuthority> authList = new LinkedList<GrantedAuthority>();

    /**
     * Setup the variable before the tests execution.
     *
     * @throws IOException
     *         In case of issues when opening the files
     */
    @Before
    public void setupBefore() throws IOException {
        authList.add(new SimpleGrantedAuthority("permission-type-cl-manage|dev|*"));
        authList.add(new SimpleGrantedAuthority("permission-type-cl|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-cl|dev|update"));
        authList.add(new SimpleGrantedAuthority("permission-type-template|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-template|dev|update"));
        authList.add(new SimpleGrantedAuthority("permission-type-filter-vf|dev|*"));
        authList.add(new SimpleGrantedAuthority("permission-type-cl-event|dev|*"));

        authentication = new UsernamePasswordAuthenticationToken(new User("admin", "", authList), "", authList);
    }

    @Test
    public void testIsUserPermittedNoException() {
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        PrincipalUtils.setSecurityContext(securityContext);

        AuthorizationController auth = new AuthorizationController ();
        assertTrue(auth.isUserPermittedNoException(new SecureServicePermission("permission-type-cl","dev","read")));
        assertTrue(auth.isUserPermittedNoException(new SecureServicePermission("permission-type-cl-manage","dev","DEPLOY")));
        assertTrue(auth.isUserPermittedNoException(new SecureServicePermission("permission-type-filter-vf","dev","12345-55555-55555-5555")));
        assertFalse(auth.isUserPermittedNoException(new SecureServicePermission("permission-type-cl","test","read")));
    }
}

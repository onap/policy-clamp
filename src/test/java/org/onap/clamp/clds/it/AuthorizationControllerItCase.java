/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
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
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.camel.Exchange;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.clamp.authorization.AuthorizationController;
import org.onap.clamp.clds.exception.NotAuthorizedException;
import org.onap.clamp.clds.service.SecureServicePermission;
import org.onap.clamp.util.PrincipalUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
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

    private PermissionTestDefaultHelper permissionTestHelper = new PermissionTestDefaultHelper();

    // @Spy
    // MockEnvironment env;

    @Autowired
    private AuthorizationController auth;

    /**
     * Setup the variable before the tests execution.
     */
    @Before
    public void setupBefore() {
        // permissionTestHelper.setupMockEnv(env);
        List<GrantedAuthority> authList = permissionTestHelper.getAuthList();

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication())
                .thenReturn(new UsernamePasswordAuthenticationToken(new User("admin", "", authList), "", authList));
        PrincipalUtils.setSecurityContext(securityContext);
    }

    @Test
    public void testIsUserPermitted() {
        assertTrue(auth.isUserPermitted(new SecureServicePermission("permission-type-cl", "dev", "read")));
        assertTrue(auth.isUserPermitted(new SecureServicePermission("permission-type-cl-manage", "dev", "DEPLOY")));
        assertTrue(auth.isUserPermitted(
                new SecureServicePermission("permission-type-filter-vf", "dev", "12345-55555-55555-5555")));
        assertFalse(auth.isUserPermitted(new SecureServicePermission("permission-type-cl", "test", "read")));
    }

    @Test
    public void testIfUserAuthorize() {
        Exchange ex = Mockito.mock(Exchange.class);
        try {
            permissionTestHelper
                    .doActionOnAllPermissions(((type, instance, action) -> auth.authorize(ex, type, instance, action)));
        } catch (NotAuthorizedException e) {
            fail(e.getMessage());
        }
    }

    @Test(expected = NotAuthorizedException.class)
    public void testIfAuthorizeThrowException() {
        Exchange ex = Mockito.mock(Exchange.class);
        auth.authorize(ex, "cl", "test", "read");
    }
}

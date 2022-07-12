/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
 * Modifications Copyright (C) 2020 Nordix Foundation.
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

package org.onap.policy.clamp.clds.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import org.apache.camel.Exchange;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.policy.clamp.authorization.AuthorizationController;
import org.onap.policy.clamp.authorization.SecureServicePermission;
import org.onap.policy.clamp.clds.exception.NotAuthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test CldsDAO calls through CldsModel and CldsEvent. This really test the DB
 * and stored procedures.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthorizationControllerItTestCase {

    @Autowired
    private AuthorizationController auth;

    private static SecurityContext sc = SecurityContextHolder.getContext();

    /**
     * Setup the variable before the tests execution.
     */
    @BeforeClass
    public static void setupBefore() {

        sc.setAuthentication(new Authentication() {
            private static final long serialVersionUID = -6282526745791629050L;

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return Arrays.asList(new SimpleGrantedAuthority(
                                new SecureServicePermission("permission-type-cl", "dev", "read").getKey()),
                        new SimpleGrantedAuthority(new SecureServicePermission("permission-type-cl-manage", "dev",
                                "DEPLOY").getKey()),
                        new SimpleGrantedAuthority(new SecureServicePermission("permission-type-filter-vf", "dev",
                                "12345-55555-55555-5555").getKey()));
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return "admin";
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean authenticatedFlag) throws IllegalArgumentException {

            }

            @Override
            public String getName() {
                return "admin";
            }
        });

    }

    @AfterClass

    public static void afterCleanup() {
        sc.setAuthentication(null);
    }

    @Test
    public void testIsUserPermitted() {
        assertEquals("admin", AuthorizationController.getPrincipalName(sc));
        assertTrue(auth.isUserPermitted(new SecureServicePermission("permission-type-cl", "dev", "read")));
        assertTrue(auth.isUserPermitted(new SecureServicePermission("permission-type-cl-manage", "dev", "DEPLOY")));
        assertTrue(auth.isUserPermitted(
                new SecureServicePermission("permission-type-filter-vf", "dev", "12345-55555-55555-5555")));
        assertFalse(auth.isUserPermitted(new SecureServicePermission("permission-type-cl", "test", "read")));
    }

    // FIXME: Look into once the anonymous user is corrected
    @Ignore
    @Test(expected = NotAuthorizedException.class)
    public void testIfAuthorizeThrowException() {
        Exchange ex = Mockito.mock(Exchange.class);
        auth.authorize(ex, "cl", "test", "read");
    }
}

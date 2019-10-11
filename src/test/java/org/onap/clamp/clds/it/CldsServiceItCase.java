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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.onap.clamp.clds.model.CldsInfo;
import org.onap.clamp.clds.service.CldsService;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
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

    private LoggingUtils util;
    private SecurityContext securityContext = mock(SecurityContext.class);
    private Authentication auth = Mockito.mock(Authentication.class);
    private UserDetails userDetails = Mockito.mock(UserDetails.class);
    private List<GrantedAuthority> authorityList = new LinkedList<GrantedAuthority>();
    /**
     * Setup the variable before the tests execution.
     *
     * @throws IOException In case of issues when opening the files
     */
    @Before
    public void setupBefore() throws IOException {
        util = Mockito.mock(LoggingUtils.class);
        Mockito.doNothing().when(util).entering(Matchers.any(HttpServletRequest.class), Matchers.any(String.class));
        cldsService.setLoggingUtil(util);

    }

    @Test
    public void testCldsInfoNotAuthorized() {
        Mockito.when(userDetails.getUsername()).thenReturn("admin");
        Mockito.when(securityContext.getAuthentication()).thenReturn(auth);
        Mockito.when(auth.getPrincipal()).thenReturn(userDetails);

        cldsService.setSecurityContext(securityContext);
        CldsInfo cldsInfo = cldsService.getCldsInfo();
        assertFalse(cldsInfo.isPermissionReadCl());
        assertFalse(cldsInfo.isPermissionReadTemplate());
        assertFalse(cldsInfo.isPermissionUpdateCl());
        assertFalse(cldsInfo.isPermissionUpdateTemplate());
    }

    @Test
    public void testCldsInfoAuthorized() throws Exception {
        Authentication authentication;
        List<GrantedAuthority> authList = new LinkedList<GrantedAuthority>();
        authList.add(new SimpleGrantedAuthority("permission-type-cl-manage|dev|*"));
        authList.add(new SimpleGrantedAuthority("permission-type-cl|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-cl|dev|update"));
        authList.add(new SimpleGrantedAuthority("permission-type-template|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-template|dev|update"));
        authList.add(new SimpleGrantedAuthority("permission-type-filter-vf|dev|*"));
        authList.add(new SimpleGrantedAuthority("permission-type-cl-event|dev|*"));
        authentication = new UsernamePasswordAuthenticationToken(new User("admin", "", authList), "", authList);

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
        assertNotNull(in);
        in.close();
        assertEquals(cldsInfo.getCldsVersion(), prop.getProperty("clds.version"));
        assertEquals(cldsInfo.getUserName(), "admin");
    }

    @Test(expected = NotAuthorizedException.class)
    public void isAuthorizedForVfTestNotAuthorized1() throws Exception {
        when(userDetails.getUsername()).thenReturn("testName");
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(auth);
        cldsService.setSecurityContext(securityContext);
        boolean res = cldsService.isAuthorizedForVf("testId");
        assertThat(res).isTrue();
    }

    @Test(expected = NotAuthorizedException.class)
    public void isAuthorizedForVfTestNotAuthorized2() throws Exception {
        when(userDetails.getUsername()).thenReturn("testName");
        when(auth.getPrincipal()).thenReturn(userDetails);
        authorityList.add(new SimpleGrantedAuthority("permission-type-filter-vf|prod|*"));
        when((List<GrantedAuthority>)auth.getAuthorities()).thenReturn(authorityList);
        when(securityContext.getAuthentication()).thenReturn(auth);
        cldsService.setSecurityContext(securityContext);
        boolean res = cldsService.isAuthorizedForVf("testId");
        assertThat(res).isTrue();
    }

    @Test(expected = NotAuthorizedException.class)
    public void isAuthorizedForVfTestNotAuthorized3() throws Exception {
        when(userDetails.getUsername()).thenReturn("testName");
        when(auth.getPrincipal()).thenReturn(userDetails);
        authorityList.add(new SimpleGrantedAuthority("permission-type-filter-vf|dev|testId2"));
        when((List<GrantedAuthority>)auth.getAuthorities()).thenReturn(authorityList);
        when(securityContext.getAuthentication()).thenReturn(auth);
        cldsService.setSecurityContext(securityContext);
        boolean res = cldsService.isAuthorizedForVf("testId");
        assertThat(res).isTrue();
    }

    @Test(expected = NullPointerException.class)
    public void isAuthorizedForVfTestNotAuthorized4() throws Exception {
        when(userDetails.getUsername()).thenReturn("testName");
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(null);
        cldsService.setSecurityContext(securityContext);
        boolean res = cldsService.isAuthorizedForVf("testId");
        assertThat(res).isTrue();
    }

    @Test
    public void isAuthorizedForVfTest1() throws Exception {
        when(userDetails.getUsername()).thenReturn("testName");
        when(auth.getPrincipal()).thenReturn(userDetails);
        authorityList.add(new SimpleGrantedAuthority("permission-type-filter-vf|*|*"));
        when((List<GrantedAuthority>)auth.getAuthorities()).thenReturn(authorityList);
        when(securityContext.getAuthentication()).thenReturn(auth);

        cldsService.setSecurityContext(securityContext);
        boolean res = cldsService.isAuthorizedForVf("testId");
        assertThat(res).isTrue();
    }

    @Test
    public void isAuthorizedForVfTest2() throws Exception {
        when(userDetails.getUsername()).thenReturn("testName");
        when(auth.getPrincipal()).thenReturn(userDetails);
        authorityList.add(new SimpleGrantedAuthority("permission-type-filter-vf|dev|*"));
        when((List<GrantedAuthority>)auth.getAuthorities()).thenReturn(authorityList);
        when(securityContext.getAuthentication()).thenReturn(auth);

        cldsService.setSecurityContext(securityContext);
        boolean res = cldsService.isAuthorizedForVf("testId");
        assertThat(res).isTrue();
    }

    @Test
    public void isAuthorizedForVfTest3() throws Exception {
        when(userDetails.getUsername()).thenReturn("testName");
        when(auth.getPrincipal()).thenReturn(userDetails);
        authorityList.add(new SimpleGrantedAuthority("permission-type-filter-vf|dev|testId"));
        when((List<GrantedAuthority>)auth.getAuthorities()).thenReturn(authorityList);
        when(securityContext.getAuthentication()).thenReturn(auth);

        cldsService.setSecurityContext(securityContext);
        boolean res = cldsService.isAuthorizedForVf("testId");
        assertThat(res).isTrue();
    }

    @Test
    public void isAuthorizedForVfTest4() throws Exception {
        when(userDetails.getUsername()).thenReturn("testName");
        when(auth.getPrincipal()).thenReturn(userDetails);
        authorityList.add(new SimpleGrantedAuthority("permission-type-filter-vf|*|testId"));
        when((List<GrantedAuthority>)auth.getAuthorities()).thenReturn(authorityList);
        when(securityContext.getAuthentication()).thenReturn(auth);

        cldsService.setSecurityContext(securityContext);
        boolean res = cldsService.isAuthorizedForVf("testId");
        assertThat(res).isTrue();
    }

    @Test
    public void getUserIdTest() throws Exception {
        when(userDetails.getUsername()).thenReturn("testName");
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(auth);

        cldsService.setSecurityContext(securityContext);
        assertThat(cldsService.getUserId()).isEqualTo("testName");
    }
}

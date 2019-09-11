/*-
* ============LICENSE_START=======================================================
* ONAP CLAMP
* Copyright (C) 2019 Samsung. All rights reserved.
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

package org.onap.clamp.clds.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Test Logging Utils.
 */
@RunWith(MockitoJUnitRunner.class)
public class LoggingUtilsTest {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(LoggingUtilsTest.class);

    private static final String SERVICE_NAME = "LogginUtilsTest: Test Entering method";

    private LoggingUtils util;

    @Before
    public void setup() {
        this.util = new LoggingUtils(logger);
    }

    @Test
    public void testEnteringLoggingUtils() {
        // given
        final String userName = "test";

        UserDetails userDetails = Mockito.mock(UserDetails.class);
        Mockito.when(userDetails.getUsername()).thenReturn(userName);

        Authentication localAuth = Mockito.mock(Authentication.class);
        Mockito.when(localAuth.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(localAuth);
        SecurityContextHolder.setContext(securityContext);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        // when
        util.entering(request, SERVICE_NAME);

        // then
        String[] keys = { OnapLogConstants.Mdcs.PARTNER_NAME, OnapLogConstants.Mdcs.ENTRY_TIMESTAMP,
                            OnapLogConstants.Mdcs.REQUEST_ID, OnapLogConstants.Mdcs.INVOCATION_ID,
                            OnapLogConstants.Mdcs.CLIENT_IP_ADDRESS, OnapLogConstants.Mdcs.SERVER_FQDN,
                            OnapLogConstants.Mdcs.INSTANCE_UUID, OnapLogConstants.Mdcs.SERVICE_NAME };
        Map<String, String> mdc = MDC.getMDCAdapter().getCopyOfContextMap();

        assertTrue(checkMapKeys(mdc, keys));
        assertEquals(userName, mdc.get(OnapLogConstants.Mdcs.PARTNER_NAME));
    }

    @Test
    public void testExistingLoggingUtils() {
        // given
        MDC.put(OnapLogConstants.Mdcs.ENTRY_TIMESTAMP,
                ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

        // when
        util.exiting("200", SERVICE_NAME, Level.INFO, OnapLogConstants.ResponseStatus.COMPLETED);

        // then
        Map<String, String> mdc = MDC.getMDCAdapter().getCopyOfContextMap();
        assertNull(mdc);
    }

    @Test
    public void testInvokeTestUtils() {
        // given
        final String targetEntity = "LoggingUtilsTest";
        final String targetServiceName = "testInvokeTestUtils";
        HttpsURLConnection secureConnection = Mockito.mock(HttpsURLConnection.class);

        // when
        secureConnection = util.invokeHttps(secureConnection, targetEntity, targetServiceName);

        // then
        assertNotNull(secureConnection);
        String[] keys = { OnapLogConstants.Mdcs.TARGET_ENTITY, OnapLogConstants.Mdcs.TARGET_SERVICE_NAME,
                            OnapLogConstants.Mdcs.INVOCATIONID_OUT, OnapLogConstants.Mdcs.INVOKE_TIMESTAMP };
        Map<String, String> mdc = MDC.getMDCAdapter().getCopyOfContextMap();

        assertTrue(checkMapKeys(mdc, keys));
        assertEquals(targetEntity, mdc.get(OnapLogConstants.Mdcs.TARGET_ENTITY));
        assertEquals(targetServiceName, mdc.get(OnapLogConstants.Mdcs.TARGET_SERVICE_NAME));
    }

    private boolean checkMapKeys(Map map, String[] keys) {
        return Arrays.stream(keys).allMatch(key -> map.get(key) != null);
    }
}

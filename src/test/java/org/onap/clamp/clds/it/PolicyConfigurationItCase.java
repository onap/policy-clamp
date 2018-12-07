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
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.config.PolicyConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test Config Policy read from application.properties.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class PolicyConfigurationItCase {

    @Autowired
    private PolicyConfiguration policyConfiguration;

    @Test
    public void testPolicyConfiguration() {
        assertNotNull(policyConfiguration.getPdpUrl1());
        assertNotNull(policyConfiguration.getPdpUrl2());
        assertNotNull(policyConfiguration.getPapUrl());
        assertNotNull(policyConfiguration.getPolicyEnvironment());
        assertNotNull(policyConfiguration.getClientId());
        assertNotNull(policyConfiguration.getClientKey());
        assertNotNull(policyConfiguration.getNotificationType());
        assertNotNull(policyConfiguration.getNotificationUebServers());
        assertEquals(8, policyConfiguration.getProperties().size());
        assertTrue(((String) policyConfiguration.getProperties().get(PolicyConfiguration.PDP_URL1))
            .contains("/pdp/ , testpdp, alpha123"));
        assertTrue(((String) policyConfiguration.getProperties().get(PolicyConfiguration.PDP_URL2))
            .contains("/pdp/ , testpdp, alpha123"));
        assertTrue(((String) policyConfiguration.getProperties().get(PolicyConfiguration.PAP_URL))
            .contains("/pap/ , testpap, alpha123"));
        assertEquals("websocket", policyConfiguration.getProperties().get(PolicyConfiguration.NOTIFICATION_TYPE));
        assertEquals("localhost",
            policyConfiguration.getProperties().get(PolicyConfiguration.NOTIFICATION_UEB_SERVERS));
        assertEquals("python", policyConfiguration.getProperties().get(PolicyConfiguration.CLIENT_ID));
        assertEquals("dGVzdA==", policyConfiguration.getProperties().get(PolicyConfiguration.CLIENT_KEY));
        assertEquals("DEVL", policyConfiguration.getProperties().get(PolicyConfiguration.ENVIRONMENT));
    }
}

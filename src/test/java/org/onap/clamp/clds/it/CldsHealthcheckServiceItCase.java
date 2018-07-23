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
 */

package org.onap.clamp.clds.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.model.CldsHealthCheck;
import org.onap.clamp.clds.service.CldsHealthcheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests HealthCheck Service.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class CldsHealthcheckServiceItCase {

    @Autowired
    private CldsHealthcheckService cldsHealthcheckService;

    @Test
    public void testGetHealthCheck() {
        ResponseEntity response = cldsHealthcheckService.gethealthcheck();
        CldsHealthCheck cldsHealthCheck = (CldsHealthCheck) response.getBody();
        assertNotNull(cldsHealthCheck);
        assertEquals("UP", cldsHealthCheck.getHealthCheckStatus());
        assertEquals("CLDS-APP", cldsHealthCheck.getHealthCheckComponent());
        assertEquals("OK", cldsHealthCheck.getDescription());
    }
}

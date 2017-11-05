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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.ws.rs.BadRequestException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.AbstractItCase;
import org.onap.clamp.clds.client.DcaeHttpConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test HTTP and HTTPS settings + redirection of HTTP to HTTPS.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = "classpath:https/https-test.properties")
public class DcaeHttpConnectionManagerItCase extends AbstractItCase {
    @Value("${server.port}")
    private String httpsPort;
    @Value("${server.http-to-https-redirection.port}")
    private String httpPort;

    @Test
    public void testHttpGet() throws Exception {
        String response = DcaeHttpConnectionManager
                .doDcaeHttpQuery("http://localhost:" + this.httpPort + "/designer/index.html", "GET", null, null, true);
        assertNotNull(response);
        // Should be a redirection so 302, so empty
        assertTrue(response.isEmpty());
    }

    @Test
    public void testHttpsGet() throws Exception {
        String response = DcaeHttpConnectionManager.doDcaeHttpQuery(
                "https://localhost:" + this.httpsPort + "/designer/index.html", "GET", null, null, true);
        assertNotNull(response);
        // Should contain something
        assertTrue(!response.isEmpty());
    }

    @Test(expected = BadRequestException.class)
    public void testHttpsGet404() throws IOException {
        DcaeHttpConnectionManager.doDcaeHttpQuery("https://localhost:" + this.httpsPort + "/designer/index1.html",
                "GET", null, null, true);
        fail("Should have raised an BadRequestException exception");
    }

    @Test(expected = BadRequestException.class)
    public void testHttpsPost404() throws IOException {
        DcaeHttpConnectionManager.doDcaeHttpQuery("https://localhost:" + this.httpsPort + "/designer/index1.html",
                "POST", "", "application/json", true);
        fail("Should have raised an BadRequestException exception");
    }

    @Test(expected = IOException.class)
    public void testHttpException() throws IOException {
        DcaeHttpConnectionManager.doDcaeHttpQuery("http://localhost:" + this.httpsPort + "/designer/index.html", "GET",
                null, null, true);
        fail("Should have raised an IOException exception");
    }
}

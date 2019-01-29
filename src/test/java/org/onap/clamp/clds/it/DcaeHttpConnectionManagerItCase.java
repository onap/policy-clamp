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
 * Modifications copyright (c) 2018 Nokia
 * ===================================================================
 * 
 */

package org.onap.clamp.clds.it;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.BadRequestException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.client.DcaeHttpConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
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
public class DcaeHttpConnectionManagerItCase {

    @Value("${server.port}")
    private String httpsPort;
    @Value("${server.http-to-https-redirection.port}")
    private String httpPort;

    @Autowired
    DcaeHttpConnectionManager dcaeHttpConnectionManager;

    private static TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            }
        }
    };

    private void enableSslNoCheck() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier allHostsValid = new HostnameVerifier() {

            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        // set the allTrusting verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    @Before
    public void setupEnvBeforeTest() throws KeyManagementException, NoSuchAlgorithmException {
        enableSslNoCheck();
    }

    @Test
    public void testHttpGet() throws Exception {
        String response = dcaeHttpConnectionManager
                .doDcaeHttpQuery("http://localhost:" + this.httpPort + "/designer/index.html", "GET", null, null);
        assertNotNull(response);
        // Should be a redirection so 302, so empty
        assertTrue(response.isEmpty());
    }

    @Test
    public void testHttpsGet() throws Exception {
        String response = dcaeHttpConnectionManager
                .doDcaeHttpQuery("https://localhost:" + this.httpsPort + "/designer/index.html", "GET", null, null);
        assertNotNull(response);
        // Should contain something
        assertTrue(!response.isEmpty());
    }

    @Test(expected = BadRequestException.class)
    public void testHttpsGet404() throws IOException {
        dcaeHttpConnectionManager.doDcaeHttpQuery("https://localhost:" + this.httpsPort + "/designer/index1.html",
                "GET", null, null);
        fail("Should have raised an BadRequestException");
    }

    @Test(expected = BadRequestException.class)
    public void testHttpsPost404() throws IOException {
        dcaeHttpConnectionManager.doDcaeHttpQuery("https://localhost:" + this.httpsPort + "/designer/index1.html",
                "POST", "", "application/json");
        fail("Should have raised an BadRequestException");
    }

    @Test(expected = BadRequestException.class)
    public void testHttpException() throws IOException {
        dcaeHttpConnectionManager.doDcaeHttpQuery("http://localhost:" + this.httpsPort + "/designer/index.html", "GET",
                null, null);
        fail("Should have raised an BadRequestException");
    }
}

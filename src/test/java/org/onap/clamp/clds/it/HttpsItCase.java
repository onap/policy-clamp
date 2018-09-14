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

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

/**
 * Test HTTP and HTTPS settings + redirection of HTTP to HTTPS.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = "classpath:https/https-test.properties")
public class HttpsItCase {

    @Value("${server.port}")
    private String httpsPort;
    @Value("${server.http-to-https-redirection.port}")
    private String httpPort;

    /**
     * Setup the variable before tests execution.
     */
    @BeforeClass
    public static void setUp() {
        try {
            // setup ssl context to ignore certificate errors
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                    throws java.security.cert.CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                    throws java.security.cert.CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[] {
                tm
            }, null);
            SSLContext.setDefault(ctx);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testDesignerIndex() throws Exception {
        RestTemplate template = new RestTemplate();
        final MySimpleClientHttpRequestFactory factory = new MySimpleClientHttpRequestFactory(new HostnameVerifier() {

            @Override
            public boolean verify(final String hostname, final SSLSession session) {
                return true;
            }
        });
        template.setRequestFactory(factory);
        ResponseEntity<String> entity = template
            .getForEntity("http://localhost:" + this.httpPort + "/designer/index.html", String.class);
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        ResponseEntity<String> httpsEntity = template
            .getForEntity("https://localhost:" + this.httpsPort + "/designer/index.html", String.class);
        assertThat(httpsEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(httpsEntity.getBody()).contains("CLDS");
    }

    @Test
    public void testSwaggerJson() throws Exception {
        RestTemplate template = new RestTemplate();
        final MySimpleClientHttpRequestFactory factory = new MySimpleClientHttpRequestFactory(new HostnameVerifier() {

            @Override
            public boolean verify(final String hostname, final SSLSession session) {
                return true;
            }
        });
        template.setRequestFactory(factory);
        ResponseEntity<String> httpsEntity = template
            .getForEntity("https://localhost:" + this.httpsPort + "/restservices/clds/v1/api-doc", String.class);
        assertThat(httpsEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(httpsEntity.getBody()).contains("swagger");
        FileUtils.writeStringToFile(
                new File("docs/swagger/swagger.json"), httpsEntity.getBody(), Charset.defaultCharset());
    }

    /**
     * Http Request Factory for ignoring SSL hostname errors. Not for production
     * use!
     */
    class MySimpleClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

        private final HostnameVerifier verifier;

        public MySimpleClientHttpRequestFactory(final HostnameVerifier verifier) {
            this.verifier = verifier;
        }

        @Override
        protected void prepareConnection(final HttpURLConnection connection, final String httpMethod)
            throws IOException {
            if (connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setHostnameVerifier(this.verifier);
            }
            super.prepareConnection(connection, httpMethod);
        }
    }
}

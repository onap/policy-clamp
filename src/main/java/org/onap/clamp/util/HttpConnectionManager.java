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
 * Modifications copyright (c) 2018 Nokia
 * ===================================================================
 *
 */

package org.onap.clamp.util;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.BadRequestException;

import org.apache.commons.io.IOUtils;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.stereotype.Component;

/**
 * This class manages the HTTP and HTTPS connections.
 */
@Component
public class HttpConnectionManager {
    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(HttpConnectionManager.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    private static final String REQUEST_FAILED_LOG = "Request Failed - response payload=";

    private String doHttpsQuery(URL url, String requestMethod, String payload, String contentType, String target,
        String userName, String password) throws IOException {
        LoggingUtils utils = new LoggingUtils(logger);
        logger.info("Using HTTPS URL:" + url.toString());
        HttpsURLConnection secureConnection = (HttpsURLConnection) url.openConnection();
        secureConnection = utils.invokeHttps(secureConnection, target, requestMethod);
        secureConnection.setRequestMethod(requestMethod);
        if (userName != null && password != null) {
            secureConnection.setRequestProperty("Authorization", "Basic "
                + Base64.getEncoder().encodeToString((userName + ":" + password).getBytes(StandardCharsets.UTF_8)));
        }
        if (payload != null && contentType != null) {
            secureConnection.setRequestProperty("Content-Type", contentType);
            secureConnection.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(secureConnection.getOutputStream())) {
                wr.writeBytes(payload);
                wr.flush();
            }
        }
        int responseCode = secureConnection.getResponseCode();
        logger.info("Response Code: " + responseCode);
        if (responseCode < 400) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(secureConnection.getInputStream()))) {
                String responseStr = IOUtils.toString(reader);
                logger.info("Response Content: " + responseStr);
                return responseStr;
            }
        } else {
            // In case of connection failure just check whether there is a
            // content or not
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(secureConnection.getErrorStream()))) {
                String responseStr = IOUtils.toString(reader);
                logger.error(REQUEST_FAILED_LOG + responseStr);
                throw new BadRequestException(responseStr);
            }
        }
    }

    private String doHttpQuery(URL url, String requestMethod, String payload, String contentType, String target,
        String userName, String password) throws IOException {
        LoggingUtils utils = new LoggingUtils(logger);
        logger.info("Using HTTP URL:" + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection = utils.invoke(connection, target, requestMethod);
        connection.setRequestMethod(requestMethod);
        if (userName != null && password != null) {
            connection.setRequestProperty("Authorization", "Basic "
                + Base64.getEncoder().encodeToString((userName + ":" + password).getBytes(StandardCharsets.UTF_8)));
        }
        if (payload != null && contentType != null) {
            connection.setRequestProperty("Content-Type", contentType);
            connection.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(payload);
                wr.flush();
            }
        }
        int responseCode = connection.getResponseCode();
        logger.info("Response Code: " + responseCode);
        if (responseCode < 400) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String responseStr = IOUtils.toString(reader);
                logger.info("Response Content: " + responseStr);
                utils.invokeReturn();
                return responseStr;
            }
        } else {
            // In case of connection failure just check whether there is a
            // content or not
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                String responseStr = IOUtils.toString(reader);
                logger.error(REQUEST_FAILED_LOG + responseStr);
                utils.invokeReturn();
                throw new BadRequestException(responseStr);
            }
        }
    }

    /**
     * This method does a HTTP/HTTPS query with parameters specified.
     *
     * @param url
     *        The string HTTP or HTTPS that mustr be used to connect
     * @param requestMethod
     *        The Request Method (PUT, POST, GET, DELETE, etc ...)
     * @param payload
     *        The payload if any, in that case an ouputstream is opened
     * @param contentType
     *        The "application/json or application/xml, or whatever"
     * @return The payload of the answer
     * @throws IOException
     *         In case of issue with the streams
     */
    public String doHttpRequest(String url, String requestMethod, String payload, String contentType, String target,
        String userName, String password) throws IOException {
        URL urlObj = new URL(url);
        if (url.contains("https://")) { // Support for HTTPS
            return doHttpsQuery(urlObj, requestMethod, payload, contentType, target, userName, password);
        } else { // Support for HTTP
            return doHttpQuery(urlObj, requestMethod, payload, contentType, target, userName, password);
        }
    }
}

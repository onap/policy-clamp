/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.config;

import java.util.Properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "clamp.config.policy")
public class PolicyConfiguration {

    public static final String PDP_URL1 = "PDP_URL1";
    public static final String PDP_URL2 = "PDP_URL2";
    public static final String PAP_URL = "PAP_URL";
    public static final String NOTIFICATION_TYPE = "NOTIFICATION_TYPE";
    public static final String NOTIFICATION_UEB_SERVERS = "NOTIFICATION_UEB_SERVERS";
    public static final String CLIENT_ID = "CLIENT_ID";
    public static final String CLIENT_KEY = "CLIENT_KEY";
    public static final String ENVIRONMENT = "ENVIRONMENT";
    private String pdpUrl1;
    private String pdpUrl2;
    private String papUrl;
    private String notificationType;
    private String notificationUebServers;
    private String clientId;
    private String clientKey;
    private String policyEnvironment;

    public String getPdpUrl1() {
        return pdpUrl1;
    }

    public void setPdpUrl1(String pdpUrl1) {
        this.pdpUrl1 = pdpUrl1;
    }

    public String getPdpUrl2() {
        return pdpUrl2;
    }

    public void setPdpUrl2(String pdpUrl2) {
        this.pdpUrl2 = pdpUrl2;
    }

    public String getPapUrl() {
        return papUrl;
    }

    public void setPapUrl(String papUrl) {
        this.papUrl = papUrl;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getNotificationUebServers() {
        return notificationUebServers;
    }

    public void setNotificationUebServers(String notificationUebServers) {
        this.notificationUebServers = notificationUebServers;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getPolicyEnvironment() {
        return policyEnvironment;
    }

    public void setPolicyEnvironment(String environment) {
        this.policyEnvironment = environment;
    }

    public Properties getProperties() {
        Properties prop = new Properties();
        prop.put(PDP_URL1, pdpUrl1);
        prop.put(PDP_URL2, pdpUrl2);
        prop.put(PAP_URL, papUrl);
        prop.put(NOTIFICATION_TYPE, notificationType);
        prop.put(NOTIFICATION_UEB_SERVERS, notificationUebServers);
        prop.put(CLIENT_ID, clientId);
        prop.put(CLIENT_KEY, clientKey);
        prop.put(ENVIRONMENT, policyEnvironment);
        return prop;
    }
}

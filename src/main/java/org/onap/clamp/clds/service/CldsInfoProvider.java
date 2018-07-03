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
 * Modifications copyright (c) 2018 Nokia
 * ===================================================================
 *
 */

package org.onap.clamp.clds.service;

import static org.onap.clamp.clds.service.CldsService.RESOURCE_NAME;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.InputStream;
import java.util.Properties;
import org.onap.clamp.clds.model.CldsInfo;
import org.onap.clamp.clds.util.ResourceFileUtil;

class CldsInfoProvider {

    private static final String CLDS_VERSION = "clds.version";
    private final CldsService cldsService;
    private final EELFLogger logger = EELFManager.getInstance().getLogger(CldsInfoProvider.class);


    CldsInfoProvider(CldsService cldsService) {
        this.cldsService = cldsService;
    }

    CldsInfo getCldsInfo(){
        CldsInfo cldsInfo = new CldsInfo();
        cldsInfo.setUserName(cldsService.getUserName());
        cldsInfo.setCldsVersion(getCldsVersionFromProps());

        cldsInfo.setPermissionReadCl(cldsService.isAuthorizedNoException(cldsService.permissionReadCl));
        cldsInfo.setPermissionUpdateCl(cldsService.isAuthorizedNoException(cldsService.permissionUpdateCl));
        cldsInfo.setPermissionReadTemplate(cldsService.isAuthorizedNoException(cldsService.permissionReadTemplate));
        cldsInfo.setPermissionUpdateTemplate(cldsService.isAuthorizedNoException(cldsService.permissionUpdateTemplate));
        return cldsInfo;
    }

    private String getCldsVersionFromProps() {
        String cldsVersion = "";
        Properties props = new Properties();
        try (InputStream resourceStream = ResourceFileUtil.getResourceAsStream(RESOURCE_NAME)) {
            props.load(resourceStream);
            cldsVersion = props.getProperty(CLDS_VERSION);
        } catch (Exception ex) {
            logger.error("Exception caught during the clds.version reading", ex);
        }
        return cldsVersion;
    }
}

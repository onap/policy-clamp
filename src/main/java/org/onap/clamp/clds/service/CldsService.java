/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.onap.clamp.clds.model.CldsInfo;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.clamp.clds.util.OnapLogConstants;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Service to save and retrieve the CLDS model attributes.
 */
@Component
public class CldsService extends SecureServiceBase {

    /**
     * The constant securityLogger.
     */
    protected static final EELFLogger securityLogger = EELFManager.getInstance().getSecurityLogger();
    /**
     * The constant logger.
     */
    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(CldsService.class);

    private final String cldsPermissionTypeFilterVf;
    private final String cldsPermissionInstance;
    /**
     * The Permission read cl.
     */
    final SecureServicePermission permissionReadCl;
    /**
     * The Permission update cl.
     */
    final SecureServicePermission permissionUpdateCl;
    /**
     * The Permission read template.
     */
    final SecureServicePermission permissionReadTemplate;
    /**
     * The Permission update template.
     */
    final SecureServicePermission permissionUpdateTemplate;
    /**
     * The Permission read tosca.
     */
    final SecureServicePermission permissionReadTosca;
    /**
     * The Permission update tosca.
     */
    final SecureServicePermission permissionUpdateTosca;

    private LoggingUtils util = new LoggingUtils(logger);

    @Autowired
    private HttpServletRequest request;

    /**
     * Instantiates a new Clds service.
     *
     * @param cldsPersmissionTypeCl      the clds persmission type cl
     * @param cldsPermissionTypeClManage the clds permission type cl manage
     * @param cldsPermissionTypeClEvent  the clds permission type cl event
     * @param cldsPermissionTypeFilterVf the clds permission type filter vf
     * @param cldsPermissionTypeTemplate the clds permission type template
     * @param cldsPermissionTypeTosca    the clds permission type tosca
     * @param cldsPermissionInstance     the clds permission instance
     */
    @Autowired
    public CldsService(
            @Value("${clamp.config.security.permission.type.cl:permission-type-cl}") String cldsPersmissionTypeCl,
            @Value("${clamp.config.security.permission.type.cl.manage:permission-type-cl-manage}")
                    String cldsPermissionTypeClManage,
            @Value("${clamp.config.security.permission.type.cl.event:permission-type-cl-event}")
                    String cldsPermissionTypeClEvent,
            @Value("${clamp.config.security.permission.type.filter.vf:permission-type-filter-vf}")
                    String cldsPermissionTypeFilterVf,
            @Value("${clamp.config.security.permission.type.template:permission-type-template}")
                    String cldsPermissionTypeTemplate,
            @Value("${clamp.config.security.permission.type.tosca:permission-type-tosca}")
                    String cldsPermissionTypeTosca,
            @Value("${clamp.config.security.permission.instance:dev}") String cldsPermissionInstance) {
        this.cldsPermissionTypeFilterVf = cldsPermissionTypeFilterVf;
        this.cldsPermissionInstance = cldsPermissionInstance;
        permissionReadCl = SecureServicePermission.create(cldsPersmissionTypeCl, cldsPermissionInstance, "read");
        permissionUpdateCl = SecureServicePermission.create(cldsPersmissionTypeCl, cldsPermissionInstance, "update");
        permissionReadTemplate = SecureServicePermission.create(cldsPermissionTypeTemplate, cldsPermissionInstance,
                "read");
        permissionUpdateTemplate = SecureServicePermission.create(cldsPermissionTypeTemplate, cldsPermissionInstance,
                "update");
        permissionReadTosca = SecureServicePermission.create(cldsPermissionTypeTosca, cldsPermissionInstance, "read");
        permissionUpdateTosca = SecureServicePermission.create(cldsPermissionTypeTosca, cldsPermissionInstance,
                "update");
    }

    /**
     * Gets clds info. CLDS IFO service will return 3 things 1. User Name 2. CLDS
     * code version that is currently installed from pom.xml file 3. User
     * permissions
     *
     * @return the clds info
     */
    public CldsInfo getCldsInfo() {
        util.entering(request, "CldsService: GET cldsInfo");
        final Date startTime = new Date();
        LoggingUtils.setTimeContext(startTime, new Date());

        CldsInfoProvider cldsInfoProvider = new CldsInfoProvider(this);
        final CldsInfo cldsInfo = cldsInfoProvider.getCldsInfo();

        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        securityLogger.info("GET cldsInfo completed");
        util.exiting("200", "Get cldsInfo success", Level.INFO, OnapLogConstants.ResponseStatus.COMPLETED);
        return cldsInfo;
    }

    /**
     * Determine if the user is authorized for a particular VF by its invariant
     * UUID.
     *
     * @param vfInvariantUuid the vf invariant uuid
     * @return boolean or throws NotAuthorizedException
     */
    public boolean isAuthorizedForVf(String vfInvariantUuid) {
        if (cldsPermissionTypeFilterVf != null && !cldsPermissionTypeFilterVf.isEmpty()) {
            SecureServicePermission permission = SecureServicePermission.create(cldsPermissionTypeFilterVf,
                    cldsPermissionInstance, vfInvariantUuid);
            return isAuthorized(permission);
        } else {
            // if CLDS_PERMISSION_TYPE_FILTER_VF property is not provided, then
            // VF filtering is turned off
            logger.warn("VF filtering turned off");
            return true;
        }
    }

    /**
     * Sets logging util.
     *
     * @param utilP the util p
     */
    // Created for the integration test
    public void setLoggingUtil(LoggingUtils utilP) {
        util = utilP;
    }
}

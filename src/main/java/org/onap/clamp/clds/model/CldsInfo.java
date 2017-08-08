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

package org.onap.clamp.clds.model;

public class CldsInfo {

    private String  userName;
    private String  cldsVersion;
    private boolean permissionReadCl;
    private boolean permissionUpdateCl;
    private boolean permissionReadTemplate;
    private boolean permissionUpdateTemplate;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCldsVersion() {
        return cldsVersion;
    }

    public void setCldsVersion(String cldsVersion) {
        this.cldsVersion = cldsVersion;
    }

    public boolean isPermissionReadCl() {
        return permissionReadCl;
    }

    public void setPermissionReadCl(boolean permissionReadCl) {
        this.permissionReadCl = permissionReadCl;
    }

    public boolean isPermissionUpdateCl() {
        return permissionUpdateCl;
    }

    public void setPermissionUpdateCl(boolean permissionUpdateCl) {
        this.permissionUpdateCl = permissionUpdateCl;
    }

    public boolean isPermissionReadTemplate() {
        return permissionReadTemplate;
    }

    public void setPermissionReadTemplate(boolean permissionReadTemplate) {
        this.permissionReadTemplate = permissionReadTemplate;
    }

    public boolean isPermissionUpdateTemplate() {
        return permissionUpdateTemplate;
    }

    public void setPermissionUpdateTemplate(boolean permissionUpdateTemplate) {
        this.permissionUpdateTemplate = permissionUpdateTemplate;
    }

}

/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.model;

import com.google.gson.annotations.Expose;
import java.util.ArrayList;
import java.util.List;
import org.onap.clamp.clds.util.ClampVersioning;

public class ClampInformation {
    @Expose
    private String userName;
    @Expose
    private String cldsVersion = ClampVersioning.getCldsVersionFromProps();
    @Expose
    List<String> allPermissions = new ArrayList<>();

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

    public List<String> getAllPermissions() {
        return allPermissions;
    }

    public void setAllPermissions(List<String> allPermissions) {
        this.allPermissions = allPermissions;
    }
}

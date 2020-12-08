/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.policy.pdpgroup;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

public class PolicyModelKey implements Serializable {

    /**
     * The serial version ID.
     */
    private static final long serialVersionUID = 3307410842013230886L;

    @Expose
    private String name;

    @Expose
    private String version;

    /**
     * Constructor.
     */
    public PolicyModelKey(String name, String version) {
        this.name = name;
        this.version = version;
    }

    /**
     * name getter.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * name setter.
     * 
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * version getter.
     * 
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * version setter.
     * 
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PolicyModelKey other = (PolicyModelKey) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            if (!name.matches(other.name)) {
                return false;
            }
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        return true;
    }
}

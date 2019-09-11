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
 * ===================================================================
 *
 */

package org.onap.clamp.loop.components.external;

import com.google.gson.annotations.Expose;

/**
 * This is a transient state reflecting the deployment status of a component. It
 * can be Policy, DCAE, or whatever... This is object is generic. Clamp is now
 * stateless, so it triggers the different components at runtime, the status per
 * component is stored here. The state level is used to re-compute the global
 * state when multiple sub states are required for that computation (generally
 * provided sequentially to the method computeState from the camel routes.
 *
 */
public class ExternalComponentState implements Comparable<ExternalComponentState> {
    @Expose
    private String stateName;
    @Expose
    private String description;
    private int stateLevel;

    /**
     * Constructor taking stateName, description and its level.
     * 
     * @param stateName   The stateName in string
     * @param description The description in string
     * @param level       The level, higher value has higher priority and can't be
     *                    down-graded
     */
    public ExternalComponentState(String stateName, String description, int level) {
        this.stateName = stateName;
        this.description = description;
        this.stateLevel = level;
    }

    public ExternalComponentState(String stateName, String description) {
        this(stateName, description, 0);
    }

    public ExternalComponentState() {
    }

    public String getStateName() {
        return stateName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return stateName;
    }

    public int getLevel() {
        return stateLevel;
    }

    public void setLevel(int priority) {
        this.stateLevel = priority;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((stateName == null) ? 0 : stateName.hashCode());
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
        ExternalComponentState other = (ExternalComponentState) obj;
        if (stateName == null) {
            if (other.stateName != null) {
                return false;
            }
        } else if (!stateName.equals(other.stateName)) {
            return false;
        }
        return true;
    }

    /**
     * This method compares this object by using the level of them.
     * 
     * @param stateToCompare The state to compare to the current object
     * @return If the one given in input has a higher level than the current object
     *         it returns -1, 1 otherwise and 0 if equals.
     */
    @Override
    public int compareTo(ExternalComponentState stateToCompare) {
        return Integer.compare(this.getLevel(), stateToCompare.getLevel());
    }

}

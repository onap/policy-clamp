/*-
 * ============LICENSE_START=======================================================
 * policy-core
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.capabilities;

/**
 * Interface for classes that supports lock/unlock operations.
 */
public interface Lockable {

    /**
     * Locks this entity.
     * 
     * @return true is the lock operation was successful, false otherwise
     */
    public boolean lock();

    /**
     * Unlocks this entity.
     * 
     * @return true is the unlock operation was successful, false otherwise
     */
    public boolean unlock();

    /**
     * Checks if this entity is locked.
     * 
     * @return true if the entity is in a locked state, false otherwise
     */
    public boolean isLocked();
}

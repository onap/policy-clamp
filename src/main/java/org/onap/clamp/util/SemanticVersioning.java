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

package org.onap.clamp.util;

/**
 * This class is the base class for object that requires semantic versioning.
 * ... This class supports also a.b.c.d... etc ... as a version.
 *
 *
 */
public class SemanticVersioning {
    public static final int BEFORE = -1;
    public static final int EQUAL = 0;
    public static final int AFTER = 1;

    /**
     * The compare method that compare arg0 to arg1.
     * 
     * @param arg0 A version in string for semantice versioning (a.b.c.d...)
     * @param arg1 A version in string for semantice versioning (a.b.c.d...)
     * @return objects (arg0, arg1) given as parameters. It returns the value: 0: if
     *         (arg0==arg1) -1: if (arg0 < arg1) 1: if (arg0 > arg1)
     */
    public static int compare(String arg0, String arg1) {

        if (arg0 == null && arg1 == null) {
            return EQUAL;
        }
        if (arg0 == null) {
            return BEFORE;
        }
        if (arg1 == null) {
            return AFTER;
        }
        String[] arg0Array = arg0.split("\\.");
        String[] arg1Array = arg1.split("\\.");

        int smalestStringLength = Math.min(arg0Array.length, arg1Array.length);

        for (int currentVersionIndex = 0; currentVersionIndex < smalestStringLength; ++currentVersionIndex) {
            if (Integer.parseInt(arg0Array[currentVersionIndex]) < Integer.parseInt(arg1Array[currentVersionIndex])) {
                return BEFORE;
            } else if (Integer.parseInt(arg0Array[currentVersionIndex]) > Integer
                    .parseInt(arg1Array[currentVersionIndex])) {
                return AFTER;
            }
            // equals, so do not return anything, continue
        }
        if (arg0Array.length == arg1Array.length) {
            return EQUAL;
        } else {
            return Integer.compare(arg0Array.length, arg1Array.length);
        }
    }
}
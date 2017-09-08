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

package org.onap.clamp.clds.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility methods supporting transforms.
 */
public class ResourceFileUtil {

    /**
     * Disable the ResourceFileUtil constructor.
     */
    private ResourceFileUtil() {

    }

    /**
     * Return resource as a Stream.
     *
     * @param name
     * @return resource - resource as stream
     */
    public static InputStream getResourceAsStream(String name) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        if (is == null) {
            throw new IllegalArgumentException("Unable to find resource: " + name);
        }
        return is;
    }

    /**
     * Return resource as a Stream.
     *
     * @param name
     * @throws IOException
     */
    public static String getResourceAsString(String name) throws IOException {
        InputStream is = getResourceAsStream(name);
        java.util.Scanner scanner = new java.util.Scanner(is);
        java.util.Scanner delimitedScanner = scanner.useDelimiter("\\A");
        String text = delimitedScanner.hasNext() ? delimitedScanner.next() : "";
        delimitedScanner.close();
        scanner.close();
        is.close();
        return text;
    }
}

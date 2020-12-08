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
 *
 */

package org.onap.clamp.clds.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Utility methods supporting resources accesses.
 */
public final class ResourceFileUtils {

    /**
     * getResourceAsStram supports the "file:" prefix as they use URL.
     * So here we want to eliminate classpath: prefix, so that this class can get
     * files from jar resource or file system.
     */

    private static final String CLASSPATH_PREFIX = "classpath:";

    /**
     * Private constructor to avoid creating instances of util class.
     */
    private ResourceFileUtils() {
    }

    /**
     * Method to access a file from the jar resource folder or file system.
     * Give the prefix "classpath:" so that it accesses the jar resource folder (default case)
     * or the prefix "file:" so that it accesses the file system.
     *
     * @param fileName The path of the resource (no prefix it will be a classpath access,
     *                 "classpath:/myfilename" or "file:/myfilename")
     * @return The file as inputStream
     */
    public static InputStream getResourceAsStream(String fileName) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                fileName.startsWith(CLASSPATH_PREFIX) ? fileName.replaceFirst(CLASSPATH_PREFIX, "") : fileName);
        if (is == null) {
            throw new IllegalArgumentException("Unable to find resource: " + fileName);
        }
        return is;
    }

    /**
     * Method to access a resource file as a string.
     * Give the prefix "classpath:" so that it accesses the jar resource folder (default case)
     * or the prefix "file:" so that it accesses the file system.
     *
     * @param fileName The path of the resource (no prefix it will be a classpath access,
     *                 "classpath:/myfilename" or "file:/myfilename")
     * @return The file as String
     * @throws IOException In case of failure to find the file.
     */
    public static String getResourceAsString(String fileName) throws IOException {
        try (InputStream is = getResourceAsStream(fileName)) {
            return streamToString(is);
        }
    }

    private static String streamToString(InputStream inputStream) {
        try (Scanner scanner = new Scanner(inputStream)) {
            Scanner delimitedScanner = scanner.useDelimiter("\\A");
            return delimitedScanner.hasNext() ? delimitedScanner.next() : "";
        }
    }
}
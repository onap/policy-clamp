/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 Ericsson. All rights reserved.
 *  Modifications Copyright (C) 2020, 2023 Nordix Foundation.
 *  Modifications Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.utils.resources;

import com.google.re2j.Pattern;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is common utility class with static methods for handling Java resources on the class path. It is an abstract
 * class to prevent any direct instantiation and private constructor to prevent extending this class.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceUtils {
    // Get a reference to the logger
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUtils.class);

    private static final Pattern SLASH_PAT = Pattern.compile("/");

    // Resource types
    private static final String FILE_PROTOCOL = "file";
    private static final String JAR_PROTOCOL = "jar";

    /**
     * Method to resolve a resource; the local file system is checked first and then the class path is checked.
     *
     * @param resourceName The resource name
     * @return A URL to a resource
     */
    public static URL getUrl4Resource(final String resourceName) {
        // Check the local fine system first
        final var urlToResource = getLocalFile(resourceName);

        // Check if this is a local file
        if (urlToResource != null) {
            return urlToResource;
        } else {
            // Resort to the class path
            return getUrlResource(resourceName);
        }
    }

    /**
     * Method to return a resource as a string. The resource can be on the local file system or in the class path. The
     * resource is resolved and loaded into a string.
     *
     * @param resourceName The resource name
     * @return A string containing the resource
     */
    public static String getResourceAsString(final String resourceName) {
        // Get the resource as a stream, we'll convert it to a string then
        // Read the stream contents, closing when done
        try (var resourceStream = getResourceAsStream(resourceName)) {
            if (resourceStream == null) {
                return null;
            }
            return IOUtils.toString(resourceStream, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            LOGGER.debug("error reading resource stream {}", resourceName, e);
            return null;
        }
    }

    /**
     * Method to return a resource as a stream. The resource can be on the local file system or in the class path. The
     * resource is resolved and returned as a stream.
     *
     * @param resourceName The resource name
     * @return A stream attached to the resource
     */
    public static InputStream getResourceAsStream(final String resourceName) {
        // Find a URL to the resource first
        final var urlToResource = getUrl4Resource(resourceName);

        // Check if the resource exists
        if (urlToResource == null) {
            // No resource found
            LOGGER.debug("could not find resource \"{}\" : ", resourceName);
            return null;
        }

        // Read the resource into a string
        try {
            return urlToResource.openStream();
        } catch (final IOException e) {
            // Any of many IO exceptions such as the resource is a directory
            LOGGER.debug("error attaching resource {}", resourceName, e);
            return null;
        }
    }

    /**
     * Method to get a URL resource from the class path.
     *
     * @param resourceName The resource name
     * @return The URL to the resource
     */
    public static URL getUrlResource(final String resourceName) {
        try {
            final var classLoader = ResourceUtils.class.getClassLoader();

            final String[] fileParts = SLASH_PAT.split(resourceName);
            // Read the resource
            var url = classLoader.getResource(resourceName);

            // Check if the resource is defined
            if (url != null) {
                // Return the resource as a file name
                LOGGER.debug("found URL resource \"{}\" : ", url);
                return url;
            } else {
                url = classLoader.getResource(fileParts[fileParts.length - 1]);
                if (url == null) {
                    LOGGER.debug("cound not find URL resource \"{}\" : ", resourceName);
                    return null;
                }
                LOGGER.debug("found URL resource \"{}\" : ", url);
                return url;
            }
        } catch (final Exception e) {
            LOGGER.debug("error getting URL resource {}", resourceName, e);
            return null;
        }
    }

    /**
     * Method to get a URL resource from the local machine.
     *
     * @param resourceName The resource name
     * @return The URL to the resource
     */
    public static URL getLocalFile(final String resourceName) {
        try {
            // Input might already be in URL format
            final var ret = new URL(resourceName);
            final var f = new File(ret.toURI());
            if (f.exists()) {
                return ret;
            }
        } catch (final Exception ignore) {
            // We ignore exceptions here and catch them below
        }

        try {
            final var f = new File(resourceName);
            // Check if the file exists
            if (f.exists()) {
                final var urlret = f.toURI().toURL();
                LOGGER.debug("resource \"{}\" was found on the local file system", f.toURI().toURL());
                return urlret;
            } else {
                LOGGER.debug("resource \"{}\" does not exist on the local file system", resourceName);
                return null;
            }
        } catch (final Exception e) {
            LOGGER.debug("error finding resource {}", resourceName, e);
            return null;
        }
    }

    /**
     * Gets the file path for a resource on the local file system or on the class path.
     *
     * @param resource the resource to the get the file path for
     * @return the resource file path
     */
    public static String getFilePath4Resource(final String resource) {
        if (resource == null) {
            return null;
        }

        var modelFileUrl = getUrl4Resource(resource);
        if (modelFileUrl != null) {
            return modelFileUrl.getPath();
        } else {
            return resource;
        }
    }

    /**
     * Read the list of entries in a resource directory.
     *
     * @param resourceDirectoryName the name of the resource directory
     * @return a set of entries
     */
    public static Set<String> getDirectoryContents(final String resourceDirectoryName) {
        // Find the location of the resource, is it in a Jar or on the local file system?
        var directoryUrl = ResourceUtils.getUrl4Resource(resourceDirectoryName);

        if (directoryUrl == null) {
            LOGGER.debug("resource \"{}\" was not found", resourceDirectoryName);
            return Collections.emptySet();
        }

        if (FILE_PROTOCOL.equals(directoryUrl.getProtocol())) {
            return getDirectoryContentsLocal(directoryUrl, resourceDirectoryName);
        } else if (JAR_PROTOCOL.equals(directoryUrl.getProtocol())) {
            // Examine the Jar
            return getDirectoryContentsJar(directoryUrl, resourceDirectoryName);
        } else {
            LOGGER.debug("resource \"{}\" has an unsupported protocol {}", resourceDirectoryName,
                    directoryUrl.getProtocol());
            return Collections.emptySet();
        }
    }

    /**
     * Get a list of the contents of a local resource directory.
     *
     * @param localResourceDirectoryUrl the local resource file URL
     * @param resourceDirectoryName the name of the resource directory
     * @return a set of the directory contents
     */
    public static Set<String> getDirectoryContentsLocal(final URL localResourceDirectoryUrl,
            final String resourceDirectoryName) {
        var localDirectory = new File(localResourceDirectoryUrl.getFile());

        if (!localDirectory.isDirectory()) {
            LOGGER.debug("resource \"{}\" is not a directory", resourceDirectoryName);
            return Collections.emptySet();
        }

        Set<String> localDirectorySet = new TreeSet<>();
        for (File localDirectoryEntry : Objects.requireNonNull(localDirectory.listFiles())) {
            if (localDirectoryEntry.isDirectory()) {
                localDirectorySet
                        .add(resourceDirectoryName + File.separator + localDirectoryEntry.getName() + File.separator);
            } else {
                localDirectorySet.add(resourceDirectoryName + File.separator + localDirectoryEntry.getName());
            }
        }

        return localDirectorySet;
    }

    /**
     * Get a list of the contents of a local resource directory.
     *
     * @param jarResourceDirectoryUrl the name of the resource directory in the jar
     * @param resourceDirectoryName the name of the resource directory
     * @return a set of the directory contents
     */
    public static Set<String> getDirectoryContentsJar(final URL jarResourceDirectoryUrl,
            final String resourceDirectoryName) {
        String dirNameWithSlash = resourceDirectoryName + "/";
        int minLength = dirNameWithSlash.length() + 1;
        var jarResourceDirectory = new File(jarResourceDirectoryUrl.getPath());
        String jarFileName = jarResourceDirectory.getParent().replaceFirst("^file:", "").replaceFirst("!.*$", "");

        Set<String> localDirectorySet = new TreeSet<>();

        try (var jarFile = new JarFile(jarFileName)) {
            Enumeration<JarEntry> entries = jarFile.entries(); // NOSONAR

            while (entries.hasMoreElements()) {
                /*
                 * Ignore sonar issue, as the entries are not being expanded here.
                 */
                JarEntry je = entries.nextElement();    // NOSONAR
                String jeName = je.getName();

                if (jeName.length() >= minLength && jeName.startsWith(dirNameWithSlash)) {
                    localDirectorySet.add(jeName);
                }
            }
        } catch (IOException ioe) {
            LOGGER.debug("error opening jar file {}", jarResourceDirectoryUrl.getPath());
            return Collections.emptySet();
        }

        return localDirectorySet;
    }
}

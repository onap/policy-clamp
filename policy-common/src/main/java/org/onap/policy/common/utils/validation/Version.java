/*-
 * ============LICENSE_START=======================================================
 * ONAP COMMON
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019 Nordix Foundation.
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

package org.onap.policy.common.utils.validation;

import com.google.re2j.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Version of an object within the model. Versions are assumed to be of the form: major or major.minor.patch, where each
 * component is numeric.
 */
@Data
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class Version implements Comparable<Version> {
    private static final Logger logger = LoggerFactory.getLogger(Version.class);

    /**
     * Pattern to match a version of the form, major or major.minor.patch, where all components are numeric.
     */
    private static final Pattern VERSION_PAT = Pattern.compile("(\\d+)([.](\\d+)[.](\\d+))?");

    private final int major;
    private final int minor;
    private final int patch;


    /**
     * String constructor.
     *
     * @param versionString the version string
     */
    public Version(@NonNull final String versionString) {
        var newVersion = makeVersion("String", "constructor", versionString);

        if (newVersion != null) {
            this.major = newVersion.major;
            this.minor = newVersion.minor;
            this.patch = newVersion.patch;
        } else {
            this.major = 0;
            this.minor = 0;
            this.patch = 0;
        }
    }

    /**
     * Creates a version object.
     *
     * @param type type of object with which the version is associated, used when logging
     * @param name name with which the version is associated, used when logging
     * @param versionText the version, in textual form
     * @return a new version, or {@code null} if the version cannot be created from the key (e.g., the key has a version
     *         that does not match the major.minor.patch form)
     */
    public static Version makeVersion(String type, String name, String versionText) {
        var matcher = VERSION_PAT.matcher(versionText);
        if (!matcher.matches()) {
            logger.info("invalid version for {} {}: {}", type, name, versionText);
            return null;
        }

        try {
            if (matcher.group(2) == null) {
                // form: major
                return new Version(Integer.parseInt(matcher.group(1)), 0, 0);

            } else {
                // form: major.minor.patch
                return new Version(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(3)),
                        Integer.parseInt(matcher.group(4)));
            }

        } catch (NumberFormatException e) {
            logger.info("invalid version for {} {}: {}", type, name, versionText, e);
            return null;
        }
    }

    /**
     * Generates a new version from a string.
     *
     * @return a new version, of the form major.0.0, where "major" is one more than "this" version's major number
     */
    public Version newVersion() {
        return new Version(major + 1, 0, 0);
    }

    @Override
    public int compareTo(Version other) {
        var result = Integer.compare(major, other.major);
        if (result != 0) {
            return result;
        }
        if ((result = Integer.compare(minor, other.minor)) != 0) {
            return result;
        }
        return Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}

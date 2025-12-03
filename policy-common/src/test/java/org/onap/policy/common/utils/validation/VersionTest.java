/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019-2024 Nordix Foundation.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VersionTest {
    private static final String TYPE = "my-type";
    private static final String NAME = "my-name";

    private static final int MAJOR = 10;
    private static final int MINOR = 2;
    private static final int PATCH = 3;

    private Version vers;

    @BeforeEach
    public void setUp() {
        vers = new Version(MAJOR, MINOR, PATCH);
    }

    @Test
    void testHashCode() {
        int hash = vers.hashCode();
        int hash2 = new Version(MAJOR, MINOR, PATCH + 1).hashCode();
        assertNotEquals(hash, hash2);
    }

    @Test
    void testConstructor() {
        Version versionTest = new Version("1.0.2");
        assertEquals(1, versionTest.getMajor());
        assertEquals(0, versionTest.getMinor());
        assertEquals(2, versionTest.getPatch());

        versionTest = new Version("null");
        assertEquals(0, versionTest.getMajor());
        assertEquals(0, versionTest.getMinor());
        assertEquals(0, versionTest.getPatch());
    }

    @Test
    void testMakeVersion() {
        assertEquals("9.8.7", Version.makeVersion(TYPE, NAME, "9.8.7").toString());
        assertEquals("9.0.0", Version.makeVersion(TYPE, NAME, "9").toString());

        assertNull(Version.makeVersion(TYPE, NAME, ""));
        assertNull(Version.makeVersion(TYPE, NAME, "a.3.4"));
        assertNull(Version.makeVersion(TYPE, NAME, "100."));
        assertNull(Version.makeVersion(TYPE, NAME, "10000000000000000.2.3"));
        assertNull(Version.makeVersion(TYPE, NAME, "1.20000000000000000.3"));
        assertNull(Version.makeVersion(TYPE, NAME, "1.2.30000000000000000"));
    }

    @Test
    void testNewVersion() {
        vers = vers.newVersion();
        assertEquals("11.0.0", vers.toString());
    }

    @Test
    void testEquals() {
        assertNotEquals(null, vers);
        assertNotEquals(vers, new Object());

        assertEquals(vers, vers);

        assertEquals(vers, new Version(MAJOR, MINOR, PATCH));

        assertNotEquals(vers, new Version(MAJOR + 1, MINOR, PATCH));
        assertNotEquals(vers, new Version(MAJOR, MINOR + 1, PATCH));
        assertNotEquals(vers, new Version(MAJOR, MINOR, PATCH + 1));
    }

    @Test
    void testCompareTo() {
        vers = new Version(101, 201, 301);

        // equals case
        assertEquals(0, new Version(101, 201, 301).compareTo(vers));

        // major takes precedence
        assertTrue(new Version(102, 200, 300).compareTo(vers) > 0);

        // minor takes precedence over patch
        assertTrue(new Version(101, 202, 300).compareTo(vers) > 0);

        // compare major
        assertTrue(new Version(100, 201, 301).compareTo(vers) < 0);
        assertTrue(new Version(102, 201, 301).compareTo(vers) > 0);

        // compare minor
        assertTrue(new Version(101, 200, 301).compareTo(vers) < 0);
        assertTrue(new Version(101, 202, 301).compareTo(vers) > 0);

        // compare patch
        assertTrue(new Version(101, 201, 300).compareTo(vers) < 0);
        assertTrue(new Version(101, 201, 302).compareTo(vers) > 0);
    }

    @Test
    void testToString() {
        assertEquals("10.2.3", vers.toString());
    }

    @Test
    void testGetMajor() {
        assertEquals(MAJOR, vers.getMajor());
    }

    @Test
    void testGetMinor() {
        assertEquals(MINOR, vers.getMinor());
    }

    @Test
    void testGetPatch() {
        assertEquals(PATCH, vers.getPatch());
    }

    @Test
    void testVersionIntIntInt() {
        assertEquals("5.6.7", new Version(5, 6, 7).toString());
    }

    @Test
    void testVersion() {
        assertEquals("0.0.0", new Version().toString());
    }
}

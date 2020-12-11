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
 */

package org.onap.policy.clamp.util;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.IOException;
import org.onap.aaf.cadi.Symm;
import org.onap.policy.clamp.clds.util.ResourceFileUtils;

/**
 * PassDecoder for decrypting the truststore and keystore password.
 */
public class PassDecoder {
    /**
     * Used to log PassDecoder class.
     */
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(PassDecoder.class);

    /**
     * Decode the password.
     *
     * @param encryptedPass The encrypted password
     * @param keyFileName       The key file name in String
     */
    public static String decode(String encryptedPass, String keyFileName) {
        if (null == keyFileName) {
            logger.debug("Key file is not defined, thus password will not be decrypted");
            return encryptedPass;
        }
        if (null == encryptedPass) {
            logger.error("Encrypted password is not defined");
            return null;
        }
        try {
            return Symm.obtain(ResourceFileUtils.getResourceAsStream(keyFileName)).depass(encryptedPass);
        } catch (IOException e) {
            logger.error("Exception occurred during the key decryption", e);
            return null;
        }
    }
}

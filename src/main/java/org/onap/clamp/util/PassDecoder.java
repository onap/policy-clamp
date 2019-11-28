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

package org.onap.clamp.util;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.onap.aaf.cadi.Symm;
import org.onap.clamp.clds.util.ResourceFileUtil;

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
     * @param encryptedPass The encrypted password
     * @param keyFileIs The key file in InputStream format
     */
    public static String decode(String encryptedPass, String keyFile) {
        if (null == keyFile) {
            logger.debug("Key file is not defined, thus password will not be decrypted");
            return encryptedPass;
        }
        if (null == encryptedPass) {
            logger.error("Encrypted password is not defined");
            return null;
        }
        try {
            InputStream is;
            if (keyFile.contains("classpath:")) {
                is = ResourceFileUtil.getResourceAsStream(keyFile.replaceAll("classpath:", ""));
            } else {
                File key = new File(keyFile);
                is = new FileInputStream(key);
            }
            Symm symm = Symm.obtain(is);

            return symm.depass(encryptedPass);
        } catch (IOException e) {
            logger.error("Exception occurred during the key decryption", e);
            return null;
        }
    }
}

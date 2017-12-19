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

package org.onap.clamp.clds.config;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.security.GeneralSecurityException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.dbcp.BasicDataSource;
import org.onap.clamp.clds.util.CryptoUtils;

/**
 * This class is an extension of the standard datasource, it will be used to
 * decode the encoded password defined in the application.properties.
 *
 */
public class EncodedPasswordBasicDataSource extends BasicDataSource {
    protected static final EELFLogger logger        = EELFManager.getInstance()
            .getLogger(EncodedPasswordBasicDataSource.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    /**
     * This method is used automatically by Spring to decode the password.
     */
    @Override
    public synchronized void setPassword(String encodedPassword) {
        try {
            this.password = CryptoUtils.decrypt(encodedPassword);
        } catch (GeneralSecurityException e) {
            logger.error("Unable to decrypt the DB password", e);
        } catch (DecoderException e) {
            logger.error("Exception caught when decoding the HEX String Key for encryption", e);
        }
    }
}
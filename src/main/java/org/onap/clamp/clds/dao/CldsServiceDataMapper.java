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

package org.onap.clamp.clds.dao;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import org.apache.commons.io.serialization.ValidatingObjectInputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.onap.clamp.clds.model.CldsServiceData;
import org.springframework.jdbc.core.RowMapper;

/**
 * Generic mapper for CldsDBServiceCache
 */
public final class CldsServiceDataMapper implements RowMapper<CldsServiceData> {
    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(CldsDao.class);

    @Override
    public CldsServiceData mapRow(ResultSet rs, int rowNum) throws SQLException {
        CldsServiceData cldsServiceData = new CldsServiceData();
        long age;
        age = rs.getLong(5);
        Blob blob = rs.getBlob(4);
        InputStream is = blob.getBinaryStream();
        try (ValidatingObjectInputStream oip = new ValidatingObjectInputStream(is)) {
        	oip.accept(CldsServiceData.class);
            cldsServiceData = (CldsServiceData) oip.readObject();
            cldsServiceData.setAgeOfRecord(age);
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Error caught while retrieving cldsServiceData from database", e);
        }
        return cldsServiceData;
    }
}

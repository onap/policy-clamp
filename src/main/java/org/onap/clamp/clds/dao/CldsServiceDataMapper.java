/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.dao;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.commons.io.serialization.ValidatingObjectInputStream;
import org.onap.clamp.clds.model.CldsServiceData;
import org.onap.clamp.clds.model.CldsVfData;
import org.onap.clamp.clds.model.CldsVfKPIData;
import org.onap.clamp.clds.model.CldsVfcData;
import org.springframework.jdbc.core.RowMapper;

/**
 * Generic mapper for CldsDBServiceCache
 */
public final class CldsServiceDataMapper implements RowMapper<CldsServiceData> {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(CldsDao.class);

    @Override
    public CldsServiceData mapRow(ResultSet rs, int rowNum) throws SQLException {
        CldsServiceData cldsServiceData = new CldsServiceData();
        try (ValidatingObjectInputStream oip = new ValidatingObjectInputStream(rs.getBlob(4).getBinaryStream())) {
            oip.accept(CldsServiceData.class, ArrayList.class, CldsVfData.class, CldsVfcData.class,
                    CldsVfKPIData.class);
            cldsServiceData = (CldsServiceData) oip.readObject();
            cldsServiceData.setAgeOfRecord(rs.getLong(5));
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Error caught while deserializing cldsServiceData from database", e);
            return null;
        }
        return cldsServiceData;
    }
}

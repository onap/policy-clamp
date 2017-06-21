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

package org.onap.clamp.clds.model;

import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.service.CldsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CldsServiceData implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(CldsServiceData.class);

    private static final long serialVersionUID = 1L;

    private String serviceInvariantUUID;
    private String serviceUUID;
    private Long ageOfRecord;
    private List<CldsVfData> cldsVfs;

    public String getServiceInvariantUUID() {
        return serviceInvariantUUID;
    }

    public void setServiceInvariantUUID(String serviceInvariantUUID) {
        this.serviceInvariantUUID = serviceInvariantUUID;
    }

    public List<CldsVfData> getCldsVfs() {
        return cldsVfs;
    }

    public void setCldsVfs(List<CldsVfData> cldsVfs) {
        this.cldsVfs = cldsVfs;
    }

    public String getServiceUUID() {
        return serviceUUID;
    }

    public void setServiceUUID(String serviceUUID) {
        this.serviceUUID = serviceUUID;
    }

    public CldsServiceData getCldsServiceCache(CldsDao cldsDao, String invariantServiceUUID) throws Exception {
        return cldsDao.getCldsServiceCache(invariantServiceUUID);
    }

    public void setCldsServiceCache(CldsDao cldsDao, CldsDBServiceCache cldsDBServiceCache) throws Exception {
        cldsDao.setCldsServiceCache(cldsDBServiceCache);
    }

    public Long getAgeOfRecord() {
        return ageOfRecord;
    }

    public void setAgeOfRecord(Long ageOfRecord) {
        this.ageOfRecord = ageOfRecord;
    }

    /**
     * Filter out any VFs that the user is not authorized for.
     * Use the CldsService to determine if the user is authorized for a VF.
     *
     * @param svc
     */
    public void filterVfs(CldsService svc) {
        List<CldsVfData> filteredCldsVfs = new ArrayList<>();
        if (cldsVfs == null) {
            logger.debug("cldsVfs == null");
        } else {
            for (CldsVfData vf : cldsVfs) {
                // if user is authorized for the VF then add it to the filtered list
                try {
                    if (svc.isAuthorizedForVf(vf.getVfInvariantResourceUUID())) {
                        filteredCldsVfs.add(vf);
                    }
                } catch (NotAuthorizedException e) {
                    logger.debug("user not authorized for {}", vf.getVfInvariantResourceUUID());
                    // when not NotAuthorizedException - don't add to filteredCldsVfs list
                }
            }
        }
        // new filtered list replaces the list of VFs for the user
        cldsVfs = filteredCldsVfs;
    }
}

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

import com.att.ajsc.common.AjscProvider;
import com.att.ajsc.common.AjscService;
import org.onap.clamp.clds.client.*;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.onap.clamp.clds.transform.XslTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("clamp-default")
public class CldsConfiguration {

    @Autowired
    private ApplicationContext context;

    /**
     * Clds Identity databse DataSource configuration
     */
    @Bean(name = "cldsDataSource")
    @ConfigurationProperties(prefix = "spring.cldsdatasource")
    public DataSource cldsDataSource() {
        return DataSourceBuilder
                .create()
                .build();
    }

    @Bean(name = "jaxrsProviders")
    public List jaxrsProviders() {
        return new ArrayList(context.getBeansWithAnnotation(AjscProvider.class).values());
    }

    @Bean(name = "jaxrsServices")
    public List jaxrsServices() {
        return new ArrayList(context.getBeansWithAnnotation(AjscService.class).values());
    }

    @Bean(name = "cldsDao")
    public CldsDao getCldsDao() {
        CldsDao cldsDao = new CldsDao();
        cldsDao.setDataSource(cldsDataSource());
        return cldsDao;
    }

    @Bean(name = "cldsBpmnTransformer")
    public XslTransformer getCldsBpmnXslTransformer() throws TransformerConfigurationException {
        XslTransformer xslTransformer = new XslTransformer();
        xslTransformer.setXslResourceName("xsl/clds-bpmn-transformer.xsl");
        return xslTransformer;
    }

    @Bean
    public RefProp getRefProp() throws IOException {
        return new RefProp();
    }

    @Bean
    public PolicyClient getPolicyClient() {
        return new PolicyClient();
    }

    @Bean(name = "cldsEventDelegate")
    public CldsEventDelegate getCldsEventDelegate() {
        return new CldsEventDelegate();
    }

    @Bean(name = "dcaeReqDelegate")
    public DcaeReqDelegate getDcaeReqDelegate() {
        return new DcaeReqDelegate();
    }

    @Bean(name = "sdcSendReqDelegate")
    public SdcSendReqDelegate getSdcSendReqDelegate() {
        return new SdcSendReqDelegate();
    }

    @Bean(name = "dcaeReqDeleteDelegate")
    public DcaeReqDeleteDelegate getDcaeReqDeleteDelegate() {
        return new DcaeReqDeleteDelegate();
    }

    @Bean(name = "operationalPolicyDelegate")
    public OperationalPolicyDelegate getOperationalPolicyDelegate() {
        return new OperationalPolicyDelegate();
    }

    @Bean(name = "operationalPolicyDeleteDelegate")
    public OperationalPolicyDeleteDelegate getOperationalPolicyDeleteDelegate() {
        return new OperationalPolicyDeleteDelegate();
    }

    @Bean(name = "stringMatchPolicyDelegate")
    public StringMatchPolicyDelegate getStringMatchPolicyDelegate() {
        return new StringMatchPolicyDelegate();
    }

    @Bean(name = "stringMatchPolicyDeleteDelegate")
    public StringMatchPolicyDeleteDelegate getStringMatchPolicyDeleteDelegate() {
        return new StringMatchPolicyDeleteDelegate();
    }

    @Bean(name = "sdcCatalogServices")
    public SdcCatalogServices getAsdcCatalogServices() {
        return new SdcCatalogServices();
    }
}
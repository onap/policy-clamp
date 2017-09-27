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

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;
import javax.xml.transform.TransformerConfigurationException;

import org.onap.clamp.clds.client.CldsEventDelegate;
import org.onap.clamp.clds.client.DcaeDispatcherServices;
import org.onap.clamp.clds.client.DcaeInventoryServices;
import org.onap.clamp.clds.client.HolmesPolicyDelegate;
import org.onap.clamp.clds.client.HolmesPolicyDeleteDelegate;
import org.onap.clamp.clds.client.OperationalPolicyDelegate;
import org.onap.clamp.clds.client.OperationalPolicyDeleteDelegate;
import org.onap.clamp.clds.client.PolicyClient;
import org.onap.clamp.clds.client.SdcCatalogServices;
import org.onap.clamp.clds.client.SdcSendReqDelegate;
import org.onap.clamp.clds.client.TcaPolicyDelegate;
import org.onap.clamp.clds.client.TcaPolicyDeleteDelegate;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.onap.clamp.clds.transform.XslTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("clamp-default")
public class CldsConfiguration {

    @Autowired
    private ApplicationContext context;

    /**
     * Clds Identity database DataSource configuration
     */
    @Bean(name = "cldsDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.cldsdb")
    public DataSource cldsDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "jaxrsProviders")
    public List<?> jaxrsProviders() {
        return new ArrayList(context.getBeansWithAnnotation(AjscProvider.class).values());
    }

    @Bean(name = "jaxrsServices")
    public List<?> jaxrsServices() {
        return new ArrayList(context.getBeansWithAnnotation(AjscService.class).values());
    }

    @Bean(name = "cldsDao")
    public CldsDao getCldsDao(@Qualifier("cldsDataSource") DataSource dataSource) {
        CldsDao cldsDao = new CldsDao();
        cldsDao.setDataSource(dataSource);
        return cldsDao;
    }

    @Bean(name = "cldsBpmnTransformer")
    public XslTransformer getCldsBpmnXslTransformer() throws TransformerConfigurationException {
        XslTransformer xslTransformer = new XslTransformer();
        xslTransformer.setXslResourceName("xsl/clds-bpmn-transformer.xsl");
        return xslTransformer;
    }

    @Bean
    public RefProp getRefProp() {
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

    @Bean(name = "sdcSendReqDelegate")
    public SdcSendReqDelegate getSdcSendReqDelegate() {
        return new SdcSendReqDelegate();
    }

    @Bean(name = "operationalPolicyDelegate")
    public OperationalPolicyDelegate getOperationalPolicyDelegate() {
        return new OperationalPolicyDelegate();
    }

    @Bean(name = "operationalPolicyDeleteDelegate")
    public OperationalPolicyDeleteDelegate getOperationalPolicyDeleteDelegate() {
        return new OperationalPolicyDeleteDelegate();
    }

    @Bean(name = "sdcCatalogServices")
    public SdcCatalogServices getSdcCatalogServices() {
        return new SdcCatalogServices();
    }

    @Bean(name = "dcaeDispatcherServices")
    public DcaeDispatcherServices getDcaeDispatcherServices() {
        return new DcaeDispatcherServices();
    }

    @Bean(name = "dcaeInventoryServices")
    public DcaeInventoryServices getDcaeInventoryServices() {
        return new DcaeInventoryServices();
    }

    @Bean(name = "tcaPolicyDelegate")
    public TcaPolicyDelegate getTcaPolicyDelegate() {
        return new TcaPolicyDelegate();
    }

    @Bean(name = "tcaPolicyDeleteDelegate")
    public TcaPolicyDeleteDelegate getTcaPolicyDeleteDelegate() {
        return new TcaPolicyDeleteDelegate();
    }

    @Bean(name = "holmesPolicyDelegate")
    public HolmesPolicyDelegate getHolmesPolicyDelegate() {
        return new HolmesPolicyDelegate();
    }

    @Bean(name = "holmesPolicyDeleteDelegate")
    public HolmesPolicyDeleteDelegate getHolmesPolicyDeleteDelegate() {
        return new HolmesPolicyDeleteDelegate();
    }

}
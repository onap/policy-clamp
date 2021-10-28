/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.controlloop.runtime.config;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.eclipse.persistence.config.BatchWriting;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.logging.SessionLog;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@EnableTransactionManagement
public class JpaConfiguration extends JpaBaseConfiguration {

    protected JpaConfiguration(DataSource dataSource, JpaProperties properties,
            ObjectProvider<JtaTransactionManager> jtaTransactionManager) {
        super(dataSource, properties, jtaTransactionManager);
    }

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        return new EclipseLinkJpaVendorAdapter();
    }

    @Override
    protected Map<String, Object> getVendorProperties() {
        return Map.of(PersistenceUnitProperties.BATCH_WRITING, BatchWriting.JDBC);
    }

    /**
     * Create EntityManagerFactory.
     *
     * @param builder EntityManagerFactoryBuilder
     * @param dataSource DataSource
     * @return LocalContainerEntityManagerFactoryBean
     */
    @Bean("entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactory(
            EntityManagerFactoryBuilder builder, DataSource dataSource,
            ClRuntimeParameterGroup clRuntimeParameterGroup) {

        return builder.dataSource(dataSource)
                .persistenceUnit(clRuntimeParameterGroup.getDatabaseProviderParameters().getPersistenceUnit())
                .properties(initJpaProperties()).build();
    }

    /**
     * create a PlatformTransactionManager.
     *
     * @param emf EntityManagerFactory
     * @return PlatformTransactionManager
     */
    @Bean
    public static PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        final var transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

    /**
     * create Jpa Properties.
     *
     * @return JpaProperties
     */
    @Bean
    @Primary
    public static JpaProperties properties(ClRuntimeParameterGroup clRuntimeParameterGroup) {
        final var jpaProperties = new JpaProperties();
        jpaProperties.setShowSql(clRuntimeParameterGroup.isShowSql());
        jpaProperties.setDatabasePlatform(clRuntimeParameterGroup.getDatabasePlatform());
        return jpaProperties;
    }

    private static Map<String, ?> initJpaProperties() {
        final Map<String, Object> ret = new HashMap<>();
        // Add any JpaProperty you are interested in and is supported by your Database and JPA implementation
        ret.put(PersistenceUnitProperties.BATCH_WRITING, BatchWriting.JDBC);
        ret.put(PersistenceUnitProperties.LOGGING_LEVEL, SessionLog.INFO_LABEL);
        ret.put(PersistenceUnitProperties.WEAVING, "false");
        ret.put(PersistenceUnitProperties.DDL_GENERATION, PersistenceUnitProperties.CREATE_ONLY);
        ret.put(PersistenceUnitProperties.DDL_GENERATION_MODE, PersistenceUnitProperties.DDL_DATABASE_GENERATION);
        return ret;
    }
}

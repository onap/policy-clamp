/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.liquibase;


/**
 * This test enables Hibernate validation during context startup.
 * Hibernate validation checks that the database schema matches the JPA entity mappings.
 * It will detect the following issues:
 * - missing tables or columns
 * - incorrect column types
 * It will NOT detect issues related to constraints (e.g. missing NOT NULL constraint),
 * nor will it detect extra tables or columns in the database.
 */
class HibernateValidationTest {

}

/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

update automationcomposition set deployState = 2 where deployState is null;
update automationcomposition set lockState = 4 where lockState is null;
update automationcomposition set name = '' where name is null;
update automationcomposition set version = '1.0.0' where version is null;
update automationcomposition set lastMsg = now() where lastMsg is null;
update automationcomposition set subState = 0 where subState is null;

ALTER TABLE automationcomposition
 ALTER COLUMN compositionid SET NOT NULL,
 ALTER COLUMN name SET DEFAULT '',
 ALTER COLUMN name SET NOT NULL,
 ALTER COLUMN version SET DEFAULT '1.0.0',
 ALTER COLUMN version SET NOT NULL,
 ALTER COLUMN deployState SET DEFAULT 2,
 ALTER COLUMN deployState SET NOT NULL,
 ALTER COLUMN lockState SET DEFAULT 4,
 ALTER COLUMN lockState SET NOT NULL,
 ALTER COLUMN SubState SET DEFAULT 0,
 ALTER COLUMN SubState SET NOT NULL,
 ALTER COLUMN lastMsg SET NOT NULL;

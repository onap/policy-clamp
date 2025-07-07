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

update automationcompositionelement set definition_name = '' where definition_name is null;
update automationcompositionelement set definition_version = '0.0.0' where definition_version is null;
update automationcompositionelement set deploystate = 2 where deploystate is null;
update automationcompositionelement set lockState = 4 where lockState is null;
update automationcompositionelement set subState = 0 where subState is null;

ALTER TABLE automationcompositionelement ALTER COLUMN definition_name SET NOT NULL;
ALTER TABLE automationcompositionelement ALTER COLUMN definition_name SET DEFAULT '';
ALTER TABLE automationcompositionelement ALTER COLUMN definition_version SET NOT NULL;
ALTER TABLE automationcompositionelement ALTER COLUMN definition_version SET DEFAULT '0.0.0';
ALTER TABLE automationcompositionelement ALTER COLUMN deploystate SET NOT NULL;
ALTER TABLE automationcompositionelement ALTER COLUMN deployState SET DEFAULT 2;
ALTER TABLE automationcompositionelement ALTER COLUMN lockState SET NOT NULL;
ALTER TABLE automationcompositionelement ALTER COLUMN lockState SET DEFAULT 4;
ALTER TABLE automationcompositionelement ALTER COLUMN substate SET NOT NULL;
ALTER TABLE automationcompositionelement ALTER COLUMN substate SET DEFAULT 0;
ALTER TABLE automationcompositionelement ALTER COLUMN outproperties SET NOT NULL;
ALTER TABLE automationcompositionelement ALTER COLUMN outproperties SET DEFAULT '{}';
ALTER TABLE automationcompositionelement ALTER COLUMN properties SET NOT NULL;
ALTER TABLE automationcompositionelement ALTER COLUMN properties SET DEFAULT '{}';

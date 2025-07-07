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

update automationcompositiondefinition set name = '' where name is null;
update automationcompositiondefinition set version  = '1.0.0' where version is null;
update automationcompositiondefinition set state  = 0 where state is null;
update automationcompositiondefinition set lastMsg = now() where lastMsg is null;
update automationcompositiondefinition set serviceTemplate = '' where serviceTemplate is null;

ALTER TABLE automationcompositiondefinition ALTER COLUMN name SET NOT NULL;
ALTER TABLE automationcompositiondefinition ALTER COLUMN name SET DEFAULT '';
ALTER TABLE automationcompositiondefinition ALTER COLUMN version SET NOT NULL;
ALTER TABLE automationcompositiondefinition ALTER COLUMN version SET DEFAULT '1.0.0';
ALTER TABLE automationcompositiondefinition ALTER COLUMN state SET NOT NULL;
ALTER TABLE automationcompositiondefinition ALTER COLUMN state SET DEFAULT 0;
ALTER TABLE automationcompositiondefinition ALTER COLUMN serviceTemplate SET NOT NULL;
ALTER TABLE automationcompositiondefinition ALTER COLUMN serviceTemplate SET DEFAULT '';
ALTER TABLE automationcompositiondefinition ALTER COLUMN lastMsg SET NOT NULL;

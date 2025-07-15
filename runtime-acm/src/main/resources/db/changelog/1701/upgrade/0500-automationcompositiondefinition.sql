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

UPDATE automationcompositiondefinition SET name = '' WHERE name IS NULL;
UPDATE automationcompositiondefinition SET version  = '1.0.0' WHERE version IS NULL;
UPDATE automationcompositiondefinition SET state  = 0 WHERE state IS NULL;
UPDATE automationcompositiondefinition SET lastMsg = now() WHERE lastMsg IS NULL;
UPDATE automationcompositiondefinition SET serviceTemplate = '' WHERE serviceTemplate IS NULL;

ALTER TABLE automationcompositiondefinition
 ALTER COLUMN name SET NOT NULL,
 ALTER COLUMN name SET DEFAULT '',
 ALTER COLUMN version SET NOT NULL,
 ALTER COLUMN version SET DEFAULT '1.0.0',
 ALTER COLUMN state SET NOT NULL,
 ALTER COLUMN state SET DEFAULT 0,
 ALTER COLUMN serviceTemplate SET NOT NULL,
 ALTER COLUMN serviceTemplate SET DEFAULT '',
 ALTER COLUMN lastMsg SET NOT NULL;

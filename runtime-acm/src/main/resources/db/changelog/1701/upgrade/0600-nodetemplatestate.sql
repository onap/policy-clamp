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

ALTER TABLE nodetemplatestate ALTER COLUMN nodeTemplate_name SET DEFAULT '';
UPDATE nodetemplatestate SET nodeTemplate_name  = '' WHERE nodeTemplate_name IS NULL;
ALTER TABLE nodetemplatestate ALTER COLUMN nodeTemplate_name SET NOT NULL;

ALTER TABLE nodetemplatestate ALTER COLUMN nodeTemplate_version SET DEFAULT '1.0.0';
UPDATE nodetemplatestate SET nodeTemplate_version = '1.0.0' WHERE nodeTemplate_version IS NULL;
ALTER TABLE nodetemplatestate ALTER COLUMN nodeTemplate_version SET NOT NULL;

ALTER TABLE nodetemplatestate ALTER COLUMN outProperties SET DEFAULT '{}';
UPDATE nodetemplatestate SET outProperties = '{}' WHERE outProperties IS NULL;
ALTER TABLE nodetemplatestate ALTER COLUMN outProperties SET NOT NULL;

ALTER TABLE nodetemplatestate ALTER COLUMN state SET DEFAULT 0;
UPDATE nodetemplatestate SET state = 0 WHERE state IS NULL;
ALTER TABLE nodetemplatestate ALTER COLUMN state SET NOT NULL;

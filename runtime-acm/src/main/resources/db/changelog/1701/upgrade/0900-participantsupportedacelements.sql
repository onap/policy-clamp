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

ALTER TABLE participantsupportedacelements ALTER COLUMN participantId SET DEFAULT '';
ALTER TABLE participantsupportedacelements ALTER COLUMN participantId SET NOT NULL;

ALTER TABLE participantsupportedacelements ALTER COLUMN typeName SET DEFAULT '';
UPDATE participantsupportedacelements SET typeName = '' WHERE typeName IS NULL;
ALTER TABLE participantsupportedacelements ALTER COLUMN typeName SET NOT NULL;

ALTER TABLE participantsupportedacelements ALTER COLUMN typeVersion SET DEFAULT '1.0.0';
UPDATE participantsupportedacelements SET typeVersion = '1.0.0' WHERE typeVersion IS NULL;
ALTER TABLE participantsupportedacelements ALTER COLUMN typeVersion SET NOT NULL;

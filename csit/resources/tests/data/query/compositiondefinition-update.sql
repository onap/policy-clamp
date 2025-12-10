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

UPDATE nodetemplatestate
  SET outproperties = '{"InternalState":"PRIMED","myParameterToUpdate":"MyTextUpdated"}'
  WHERE nodetemplatestateid = '3d34ca20-1b16-4f2c-971a-6ed52a0dafff';

UPDATE automationcompositiondefinition
  SET revisionid = 'adc5e211-84a7-448f-8749-78a195f51f64'
  WHERE compositionid = '6c1cf107-a2ca-4485-8129-02f9fae64d64';

/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights
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
 *
 */
import React from 'react';
import ToscaViewer from './ToscaViewer';
import { shallow } from 'enzyme';

describe('Verify ToscaViewer', () => {
    const fs = require('fs');
    const toscaYaml = fs.readFileSync('src/components/dialogs/Policy/toscaData.test.yaml', {encoding:'utf8', flag:'r'});

	beforeEach(() => {
		fetch.resetMocks();
		fetch.mockImplementation(() => {
			return Promise.resolve({
				ok: true,
				status: 200,
				json: () => {
					return Promise.resolve(toscaYaml);
			}});
		});
	})

    const toscaData = {
      "policyModelType": "onap.policies.controlloop.Guard",
      "version": "1.0.0",
      "policyAcronym": "Guard",
      "createdDate": "2021-04-09T02:29:31.407356Z",
      "updatedDate": "2021-04-09T02:29:31.407356Z",
      "updatedBy": "Not found",
      "createdBy": "Not found",
      "tableData": {
        "id": 0
      }
    };

	it('Test the render method', () => {
	    console.info(toscaYaml);
		const component = shallow(<ToscaViewer toscaData={toscaData}/>)
		expect(component).toMatchSnapshot();
	});
});
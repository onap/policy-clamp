/*-
* ============LICENSE_START=======================================================
* ONAP CLAMP
* ================================================================================
* Copyright (C) 2019 AT&T Intellectual Property. All rights
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
import Table from 'react-bootstrap/Table';
import './LoopStatus.css';

export default class LoopStatus extends React.Component {
  render() {
  	return (
      <div>
        <span id="status_clds" className="status_title">Status:
          <span className="status">&nbsp;&nbsp;&nbsp;TestStatus&nbsp;&nbsp;&nbsp;</span>
        </span>

        <div className="status_table">
          <Table striped hover>
            <thead>
                <tr>
                  <th><span align="left" className="text">ComponentState</span></th>
                  <th><span align="left" className="text">Description</span></th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td className="row_30_per">long test State</td>
                  <td className="row_70_per">test description very very very long description</td>
                </tr>
              </tbody>
            </Table>
          </div>
      </div>
  	);
  }
}


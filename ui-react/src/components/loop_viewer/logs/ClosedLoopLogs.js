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
import './ClosedLoopLogs.css';

export default class ClosedLoopViewLogs extends React.Component {
  render() {
    return (
      <div className="log_div">
        <div className="log_table">
          <label className="table_header">Loop Logs</label>
          <Table striped hover id="loop-log-div">
            <thead>
  							<tr>
  								<th><span align="left" className="text">Date</span></th>
  								<th><span align="left" className="text">Type</span></th>
  								<th><span align="left" className="text">Component</span></th>
  								<th><span align="right" className="text">Log</span></th>
  							</tr>
  						</thead>
  						<tbody>
  							<tr>
  								<td className="row_10_per">test</td>
  								<td className="row_10_per">test</td>
  								<td className="row_10_per">test</td>
  								<td className="row_70_per">test</td>
  							</tr>
  						</tbody>
            </Table>
          </div>
        </div>
    );
  }
}

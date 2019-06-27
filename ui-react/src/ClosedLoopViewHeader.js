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
import './css/index.css';

class ClosedLoopViewHeader extends React.Component {
  render() {
  	return (
      <div className="cl_panel_heading clearfix">
        <div className="color_white">
          <span className="pull-left"> <span id="modeler_name">Closed
              Loop Modeler</span>
          </span> <span className="pull-right"> <span id="loop_name">No
              LOOP loaded yet</span> &nbsp; <i ></i>
          </span>
        </div>
      </div>
  	);
  }
}

export default ClosedLoopViewHeader;

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
import ClosedLoopSVG from './ClosedLoopSVG';
import ClosedLoopLogs from './ClosedLoopLogs';
import ClosedLoopStatus from './ClosedLoopStatus';
import './css/index.css';

 class ClosedLoopViewBody extends React.Component {

   constructor(props) {
      super(props);
      this.state = {
         disableDiv: false
      };
      this.disableDiv = this.disableDiv.bind(this);
      this.enableDiv = this.enableDiv.bind(this);
    }

    disableDiv() {
      this.setState({
         disableDiv:true
      });
    }

    enableDiv() {
      this.setState({
         disableDiv:false
      });
    }


   render() {
     var divStyle = {
      display:this.state.disableDiv?'block':'none'
    };
   	return (
        <div id="paletteDiv" className="cl_view_body" style={divStyle}>
          <div id="js-canvas" className="js_canvas">
            <ClosedLoopSVG />
            <ClosedLoopLogs />
          </div>
          <ClosedLoopStatus />
        </div>
   	);
   }
 }


 export default ClosedLoopViewBody;

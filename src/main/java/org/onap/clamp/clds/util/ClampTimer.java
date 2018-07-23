/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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
package org.onap.clamp.clds.util;

import java.util.Timer;
import java.util.TimerTask;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Define the ClampTimer and CleanupTask, to clear up the Spring Authenticataion info when time is up.
 */

public class ClampTimer {
    protected static final EELFLogger logger          = EELFManager.getInstance().getLogger(ClampTimer.class);
    Timer timer;

    public ClampTimer(int seconds) {
        timer = new Timer();
        timer.schedule(new CleanupTask(), seconds*1000);
    }

    class CleanupTask extends TimerTask {
        public void run() {
            logger.debug("Time is up, clear the Spring authenticataion settings");
            //Clear up the spring authentication
            SecurityContextHolder.getContext().setAuthentication(null);
            //Terminate the timer thread
            timer.cancel(); 
        }
    }
}
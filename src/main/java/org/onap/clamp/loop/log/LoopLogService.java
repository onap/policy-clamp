/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
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

package org.onap.clamp.loop.log;

import org.onap.clamp.loop.Loop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoopLogService {

    private final LoopLogRepository repository;

    @Autowired
    public LoopLogService(LoopLogRepository repository) {
        this.repository = repository;
    }

    public void addLog(String message, String logType, Loop loop) {
        this.addLogForComponent(message, logType, "CLAMP", loop);
    }

    public void addLogForComponent(String message, String logType, String component, Loop loop) {
        loop.addLog(repository.save(new LoopLog(message, LogType.valueOf(logType), component, loop)));
    }

    public boolean isExisting(Long logId) {
        return repository.existsById(logId);
    }
}

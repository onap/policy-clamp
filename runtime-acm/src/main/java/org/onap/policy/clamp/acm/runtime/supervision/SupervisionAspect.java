/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.runtime.supervision;

import java.io.Closeable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class SupervisionAspect implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionAspect.class);

    private final SupervisionScanner supervisionScanner;
    private final SupervisionParticipantScanner participantScanner;

    private final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(1, 1, 0L,
                    TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new AcmThreadFactory());

    @Scheduled(
            fixedRateString = "${runtime.participantParameters.heartBeatMs}",
            initialDelayString = "${runtime.participantParameters.heartBeatMs}")
    public void schedule() {
        LOGGER.info("Add scheduled scanning");
        executor.execute(this::executeScan);
    }

    private void executeScan() {
        supervisionScanner.run();
        participantScanner.run();
    }

    /**
     * Intercept Messages from participant and run Supervision Scan.
     */
    @After("@annotation(MessageIntercept)")
    public void doCheck() {
        if (executor.getQueue().size() < 2) {
            LOGGER.info("Add scanning Message");
            executor.execute(supervisionScanner::run);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}

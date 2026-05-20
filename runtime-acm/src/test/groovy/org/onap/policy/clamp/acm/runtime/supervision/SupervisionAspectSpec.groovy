/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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
package org.onap.policy.clamp.acm.runtime.supervision

import org.onap.policy.clamp.acm.runtime.helper.SupervisionAspectTestHelper
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class SupervisionAspectSpec extends Specification {

    def "schedule should trigger both scanners"() {
        given:
        def supervisionScanner = Mock(SupervisionScanner)
        def participantScanner = Mock(SupervisionParticipantScanner)
        def aspect = SupervisionAspectTestHelper.createAspect(
                supervisionScanner, participantScanner)

        when:
        aspect.schedule()
        SupervisionAspectTestHelper.waitForExecution()

        then:
        1 * supervisionScanner.run()
        1 * participantScanner.run()

        cleanup:
        aspect.close()
    }

    def "doCheck called twice should trigger scanner twice"() {
        given:
        def supervisionScanner = Mock(SupervisionScanner)
        def participantScanner = Mock(SupervisionParticipantScanner)
        def aspect = SupervisionAspectTestHelper.createAspect(
                supervisionScanner, participantScanner)

        when:
        aspect.doCheck()
        aspect.doCheck()
        SupervisionAspectTestHelper.waitForExecution()

        then:
        2 * supervisionScanner.run()

        cleanup:
        aspect.close()
    }

    def "doCheck should skip execution when queue is full"() {
        given:
        def latch = new CountDownLatch(1)
        def callCount = new AtomicInteger(0)
        def supervisionScanner = Stub(SupervisionScanner) {
            run() >> {
                callCount.incrementAndGet()
                if (callCount.get() == 1) {
                    latch.await()
                }
            }
        }
        def participantScanner = Mock(SupervisionParticipantScanner)
        def aspect = SupervisionAspectTestHelper.createAspect(
                supervisionScanner, participantScanner)

        when: "first doCheck blocks the executor, next fills queue"
        aspect.doCheck()
        Thread.sleep(50)
        aspect.doCheck()
        aspect.doCheck()
        aspect.doCheck()
        latch.countDown()
        SupervisionAspectTestHelper.waitForExecution()

        then: "4th call is skipped since queue already has 2 items"
        callCount.get() <= 3

        cleanup:
        aspect.close()
    }
}

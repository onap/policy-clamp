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

/**
 * options.js — shared k6 execution options.
 *
 * Import and re-export `options` in each test file:
 *
 *   import { functionalOptions } from './lib/options.js';
 *   export const options = functionalOptions;
 *
 * Two profiles are provided:
 *
 *   functionalOptions  — 1 VU, 1 iteration; mirrors Robot CSIT behaviour
 *                        (sequential, pass/fail, no load)
 *
 *   performanceOptions — ramp-up load profile for SLA / staging tests;
 *                        equivalent to what Ericsson uses for product staging
 */

/**
 * Functional / regression profile.
 * Runs each test scenario exactly once, sequentially.
 * Thresholds enforce that every check must pass (100% pass rate).
 */
export const functionalOptions = {
    vus:        1,
    iterations: 1,

    thresholds: {
        checks: ['rate==1.0'],
        http_req_duration: ['p(95)<10000'],
    },
};

/**
 * Performance / staging profile.
 * Ramps up virtual users to simulate concurrent ACM clients.
 * Thresholds enforce SLA targets.
 *
 * Adjust VU counts and durations to match your environment capacity.
 */
export const performanceOptions = {
    scenarios: {
        ramp_up: {
            executor:          'ramping-vus',
            startVUs:          0,
            gracefulRampDown:  '30s',
            stages: [
                { duration: '1m',  target: 5  },   // warm-up
                { duration: '3m',  target: 10 },   // sustained load
                { duration: '1m',  target: 0  },   // ramp-down
            ],
        },
    },

    thresholds: {
        checks: ['rate>0.99'],
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

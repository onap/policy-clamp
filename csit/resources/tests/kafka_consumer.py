#!/usr/bin/env python3
#
# ============LICENSE_START====================================================
#  Copyright (C) 2023-2024 Nordix Foundation.
# =============================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END======================================================

# Python utility to fetch kafka topic and look for required messages.
# Accepts the arguments {topic_name} and {list of expected values} and {timeout} to verify the kafka topic.


from confluent_kafka import Consumer, KafkaException
import sys
import time


def consume_kafka_topic(topic, expected_msg, sec_timeout, bootstrap_server):
    config = {
        'bootstrap.servers': bootstrap_server,
        'group.id': 'testgrp',
        'auto.offset.reset': 'earliest'
    }
    consumer = Consumer(config)
    consumer.subscribe([topic])
    try:
        start_time = time.time()
        while time.time() - start_time < sec_timeout:
            msg = consumer.poll(1.0)
            if msg is None:
                continue
            if msg.error():
                raise KafkaException(msg.error())
            else:
                # Message received
                message = msg.value().decode('utf-8')
                if expected_msg in message:
                    print(message)
                    sys.exit(200)
    finally:
        consumer.close()


if __name__ == '__main__':
    topic_name = sys.argv[1]
    timeout = int(sys.argv[2])  # timeout in seconds for verifying the kafka topic
    expected_values = sys.argv[3]
    server = sys.argv[4]
    consume_kafka_topic(topic_name, expected_values, timeout, server)

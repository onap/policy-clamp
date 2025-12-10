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

# Python utility to produce a message on a kafka topic
# Accepts the arguments {topic_name} and {message}

from confluent_kafka import Producer
import sys


def post_to_kafka(topic, message, bootstrap_server):
    conf = {'bootstrap.servers': bootstrap_server}

    producer = Producer(conf)
    try:
        producer.produce(topic, value=message.encode('utf-8'))
        producer.flush()
        print('Message posted to Kafka topic: {}'.format(topic))
    except Exception as e:
        print('Failed to post message: {}'.format(str(e)))
    finally:
        producer.flush()


if __name__ == '__main__':
    post_to_kafka(sys.argv[1], sys.argv[2], sys.argv[3])

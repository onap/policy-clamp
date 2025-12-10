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

# Python utility to create a new kafka topic
# Accepts the argument {topic_name}

from confluent_kafka.admin import AdminClient, NewTopic
import sys


def create_topic(bootstrap_servers, topic_name, num_partitions=2, replication_factor=2):
    admin_client = AdminClient({'bootstrap.servers': bootstrap_servers})

    # Define the topic configuration
    new_topic = NewTopic(topic_name, num_partitions=num_partitions, replication_factor=replication_factor)

    # Create the topic
    admin_client.create_topics([new_topic])


if __name__ == '__main__':
    topic = sys.argv[1]
    servers = sys.argv[2]

    create_topic(servers, topic)

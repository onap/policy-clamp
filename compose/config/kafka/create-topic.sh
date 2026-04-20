#!/bin/bash -xv
# Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# blocks until kafka is reachable
kafka-topics --bootstrap-server kafka:9092 --list

echo -e 'Creating kafka topics'
kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic policy-acruntime-participant --replication-factor 1 --partitions 10
kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic acm-ppnt-sync --replication-factor 1 --partitions 10

echo -e 'Successfully created the following topics:'
kafka-topics --bootstrap-server kafka:9092 --list

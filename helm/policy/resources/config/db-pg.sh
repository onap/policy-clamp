#!/bin/bash -xv
# Copyright (C) 2025 Nordix Foundation. All rights reserved
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
#         SPDX-License-Identifier: Apache-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


export PGPASSWORD=${PG_PASSWORD}  # Set the password

psql -h ${PG_HOST} -p ${PG_PORT} -U "${PG_USER}" -d postgres --command "CREATE USER \"${PG_USER}\" WITH PASSWORD '${PG_PASSWORD}';"

# Loop through the databases to create and set permissions
for db in clampacm
do
    # Create the database
    psql -h ${PG_HOST} -p ${PG_PORT} -U "${PG_USER}" -d postgres --command "CREATE DATABASE ${db};"

    # Alter database owner
    psql -h ${PG_HOST} -p ${PG_PORT} -U "${PG_USER}" -d postgres --command "ALTER DATABASE ${db} OWNER TO \"${PG_USER}\";"

    # Grant all privileges on the database
    psql -h ${PG_HOST} -p ${PG_PORT} -U "${PG_USER}" -d postgres --command "GRANT ALL PRIVILEGES ON DATABASE ${db} TO \"${PG_USER}\";"
done


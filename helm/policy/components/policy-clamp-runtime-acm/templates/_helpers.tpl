{{/*
#
# ============LICENSE_START=======================================================
#  Copyright (C) 2024,2026 OpenInfra Foundation Europe. All rights reserved.
# ================================================================================
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
# ============LICENSE_END=========================================================
#
*/}}

{{/*
This helper defines which exporter port must be used depending on protocol
*/}}
{{- define "policy-clamp-runtime-acm.exporter-port" }}
  {{- $jaegerExporterPort := .Values.jaeger.collector.portOtlpGrpc -}}
    {{- if .Values.jaeger.collector.protocol -}}
      {{- if eq .Values.jaeger.collector.protocol "http" -}}
        {{- $jaegerExporterPort = .Values.jaeger.collector.portOtlpHttp -}}
      {{- end -}}
    {{- end -}}
  {{- $jaegerExporterPort -}}
{{- end -}}

{{/*
This helper defines whether Jaeger is enabled or not.
*/}}
{{- define "policy-clamp-runtime-acm.jaeger-enabled" }}
  {{- $jaegerEnabled := "false" -}}
  {{- if .Values.jaeger -}}
    {{- if .Values.jaeger.enabled -}}
        {{- $jaegerEnabled = .Values.jaeger.enabled -}}
    {{- end -}}
  {{- end -}}
  {{- $jaegerEnabled -}}
{{- end -}}

{{/*
This helper defines whether jaeger is using http or grpc protocol
*/}}
{{- define "policy-clamp-runtime-acm.jaeger-protocol" }}
  {{- $protocol := "grpc" -}}
  {{- if eq .Values.jaeger.collector.protocol "http" -}}
      {{- $protocol = "http/protobuf" -}}
  {{- end -}}
  {{- $protocol -}}
{{- end -}}

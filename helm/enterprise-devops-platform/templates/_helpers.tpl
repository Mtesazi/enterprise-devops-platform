{{- define "enterprise-devops-platform.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "enterprise-devops-platform.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- include "enterprise-devops-platform.name" . -}}
{{- end -}}
{{- end -}}

{{- define "enterprise-devops-platform.namespace" -}}
{{- default .Release.Namespace .Values.global.namespaceOverride -}}
{{- end -}}

{{- define "enterprise-devops-platform.labels" -}}
app.kubernetes.io/name: {{ include "enterprise-devops-platform.name" . }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "enterprise-devops-platform.selectorLabels" -}}
app.kubernetes.io/name: {{ include "enterprise-devops-platform.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

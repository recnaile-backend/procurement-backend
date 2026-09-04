{{- define "procurement-service.labels" -}}
app.kubernetes.io/name: {{ .Values.serviceName }}
app.kubernetes.io/part-of: procurement-inventory-platform
app.kubernetes.io/managed-by: Helm
platform.procurement/tier: {{ .Values.tier }}
{{- end -}}

{{- define "procurement-service.selectorLabels" -}}
app.kubernetes.io/name: {{ .Values.serviceName }}
{{- end -}}

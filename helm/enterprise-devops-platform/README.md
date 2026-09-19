# enterprise-devops-platform Helm chart

Initial platform chart structure for the current microservice stack, with dependency hooks, Secret-backed sensitive settings, and dev/prod overlays.

## Included in this first scaffold

- Config Server
- Discovery Server
- Gateway Service
- Auth Service
- Employee Service
- Department Service
- Notification Service
- Audit Service
- Payroll Service

## Included in this branch completion

- Bitnami dependency definitions for PostgreSQL and Kafka
- Kubernetes Secret template for JWT and shared database credentials
- `values-dev.yaml` for local cluster bring-up
- `values-prod.yaml` for externally managed dependencies and scaled services

## Included in `feature/platform-observability-on-k8s`

- `ServiceMonitor` per platform service, scraping `/actuator/prometheus`
  (already exposed via Micrometer + `platform-starter` on every service).
- A `PrometheusRule` with alerts for service downtime, elevated HTTP 5xx
  rate, high p95 latency, and high JVM heap usage.
- A Grafana dashboard (`dashboards/platform-services.json`), loaded via a
  labeled `ConfigMap` for dashboard-sidecar auto-discovery, covering service
  up/down, request rate, error rate, p95 latency, JVM heap, and CPU usage per
  service (with a `$service` template variable to select one or all).

All three are gated behind `monitoring.serviceMonitor.enabled`,
`monitoring.prometheusRule.enabled`, and `monitoring.grafanaDashboards.enabled`
(all `false` by default). The `ServiceMonitor`/`PrometheusRule` templates
additionally check `.Capabilities.APIVersions` for the
`monitoring.coreos.com/v1` CRDs, so enabling them on a cluster without the
Prometheus Operator installed is a no-op instead of a template/apply error.

Enable for a cluster that already runs kube-prometheus-stack and a Grafana
dashboard sidecar:

```bash
helm upgrade --install enterprise-platform ./helm/enterprise-devops-platform \
  -f ./helm/enterprise-devops-platform/values-dev.yaml \
  --set monitoring.serviceMonitor.enabled=true \
  --set monitoring.prometheusRule.enabled=true \
  --set monitoring.grafanaDashboards.enabled=true \
  --set monitoring.serviceMonitor.labels.release=kube-prometheus-stack
```

(Set `monitoring.serviceMonitor.labels`/`monitoring.prometheusRule.labels` to
whatever label your Prometheus Operator's `serviceMonitorSelector`/
`ruleSelector` requires — commonly `release: <prometheus-operator-release-name>`.)

## Still expected next

- Redis
- service-specific ConfigMaps and Secret externalization strategy

Argo CD GitOps resources (AppProject, app-of-apps, per-environment
Applications, and environment bootstrap manifests) now live under
`gitops/`. See `gitops/README.md` for the repository layout, sync-wave
ordering, and the promotion/rollback path.

## Dependency model

- `postgresql.enabled=true` uses the bundled Bitnami PostgreSQL dependency
- `kafka.enabled=true` uses the bundled Bitnami Kafka dependency
- production can disable both and point services to externally managed infrastructure

## Secrets model

- dev can use the generated `platform-secrets` Secret
- prod should set `secrets.create=false` and provide the named Secret out of band

## Examples

Development:

```bash
helm dependency update ./helm/enterprise-devops-platform
helm install enterprise-platform ./helm/enterprise-devops-platform \
  --namespace enterprise-platform \
  --create-namespace \
  -f ./helm/enterprise-devops-platform/values-dev.yaml
```

Production-style render:

```bash
helm template enterprise-platform ./helm/enterprise-devops-platform \
  -f ./helm/enterprise-devops-platform/values-prod.yaml
```

# Next Branch Plan

## Completed by `feature/kubernetes`

- first platform Helm chart scaffold for all current services
- dependency hooks for PostgreSQL and Kafka
- Secret-backed sensitive runtime values
- dev/prod values overlays and chart usage documentation

## Completed by `feature/gitops-bootstrap`

- Argo CD `AppProject`, app-of-apps `Application` manifests, and per-environment
  child `Application` resources for the platform chart (dev and prod), under `gitops/argocd/`.
- GitOps repository layout documented in `gitops/README.md`; dev/prod values
  stay in the Helm chart itself (`values-dev.yaml`/`values-prod.yaml`) and are
  selected per environment via `Application.spec.source.helm.valueFiles`.
- Sync-wave ordering: shared prerequisites (`-2`), per-environment namespace
  (`-1`), platform chart release (`0`), and — inside the chart —
  secrets/dependencies (`0`), services (`1`), ingress (`2`).
- Environment bootstrap manifests under `gitops/bootstrap/` for namespaces
  (dev/prod), the `external-secrets` namespace, and ingress prerequisites
  (`ingress-nginx` namespace + `nginx` `IngressClass`), with cluster-scoped
  resources isolated in `gitops/bootstrap/shared` to avoid ownership conflicts
  between dev and prod Applications.
- Promotion path (dev → prod) and rollback steps documented in `gitops/README.md`.

## Recommended next branch

`feature/aws-runtime-foundation`

## Completed by `feature/platform-observability-on-k8s`

- Per-service `ServiceMonitor` resources scraping `/actuator/prometheus`
  (already exposed by every service via Micrometer + `platform-starter`).
- A `PrometheusRule` with alerts for service downtime, elevated HTTP 5xx
  rate, high p95 latency, and high JVM heap usage.
- A Grafana dashboard (service up/down, request rate, error rate, p95
  latency, JVM heap, CPU) loaded via a sidecar-discoverable `ConfigMap`.
- All of the above gated behind `monitoring.*.enabled` chart values (default
  `false`) plus a `.Capabilities.APIVersions` CRD check, so clusters without
  the Prometheus Operator installed are unaffected.
- Sync-wave model extended: observability resources join platform services
  at wave `1` (documented in `gitops/README.md`).

## Exact scope for the next branch (`feature/aws-runtime-foundation`)

1. Connect the chart's runtime assumptions (Postgres, Kafka, ingress) to
   Terraform-managed AWS infrastructure (e.g. RDS, MSK, ALB/Route53) for
   environments that disable the bundled `postgresql`/`kafka` subcharts.
2. Document the split of responsibility between Terraform (cloud
   infrastructure) and Helm/Argo CD (in-cluster workloads).
3. Wire environment-specific connection details (endpoints, credentials)
   into the existing Secret/values-overlay model without breaking the
   dev path (bundled subcharts still enabled for local/dev clusters).

## Branch sequence after `feature/aws-runtime-foundation`

1. `feature/external-secrets-integration` — replace chart-managed secrets with cluster secret operators or cloud secret managers, using the `external-secrets` namespace reserved in `gitops/bootstrap/shared`.
2. `feature/redis-runtime` — add Redis runtime wiring where platform services start consuming it in Kubernetes, following the same `commonAnnotations` sync-wave pattern used for PostgreSQL/Kafka.

## Definition of done for `feature/platform-observability-on-k8s`

- [x] ServiceMonitors scrape every enabled platform service's Prometheus endpoint.
- [x] Alert rules cover service availability, error rate, latency, and JVM heap.
- [x] A starter Grafana dashboard is shipped and auto-loadable via ConfigMap.
- [x] Enabling monitoring is a single set of chart values; disabling (default) has zero effect on clusters without the Prometheus Operator.

## Definition of done for `feature/gitops-bootstrap`

- [x] Argo CD can reconcile the platform chart for at least one environment.
- [x] Values overlays are consumable without manual manifest editing.
- [x] Dependency ordering is explicit for namespace, secrets, dependencies, and services.
- [x] Promotion and rollback steps are documented for the GitOps flow.

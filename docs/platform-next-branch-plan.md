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

`feature/platform-observability-on-k8s`

## Exact scope for the next branch

1. Wire Prometheus scraping, Grafana dashboards, and `ServiceMonitor`/`PodMonitor`
   resources for the platform services, gated behind a chart value so clusters
   without the Prometheus Operator installed are unaffected.
2. Add dashboards/alerts specific to the employee-management domain (service
   health, JVM metrics, HTTP latency/error rate per service).
3. Document how observability resources fit into the existing sync-wave model.

## Branch sequence after `feature/platform-observability-on-k8s`

1. `feature/aws-runtime-foundation` — connect the chart assumptions to Terraform-managed AWS runtime infrastructure.
2. `feature/external-secrets-integration` — replace chart-managed secrets with cluster secret operators or cloud secret managers, using the `external-secrets` namespace reserved in `gitops/bootstrap/shared`.
3. `feature/redis-runtime` — add Redis runtime wiring where platform services start consuming it in Kubernetes, following the same `commonAnnotations` sync-wave pattern used for PostgreSQL/Kafka.

## Definition of done for `feature/gitops-bootstrap`

- [x] Argo CD can reconcile the platform chart for at least one environment.
- [x] Values overlays are consumable without manual manifest editing.
- [x] Dependency ordering is explicit for namespace, secrets, dependencies, and services.
- [x] Promotion and rollback steps are documented for the GitOps flow.

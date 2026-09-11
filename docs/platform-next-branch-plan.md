# Next Branch Plan

## Completed by `feature/kubernetes`

- first platform Helm chart scaffold for all current services
- dependency hooks for PostgreSQL and Kafka
- Secret-backed sensitive runtime values
- dev/prod values overlays and chart usage documentation

## Recommended next branch

`feature/gitops-bootstrap`

## Exact scope for the next branch

1. Add Argo CD `Application` manifests for the platform chart and environment promotion flow.
2. Define the GitOps repository layout for dev and prod values consumption.
3. Add sync-wave ordering for prerequisites, platform services, and ingress.
4. Add environment bootstrap manifests for namespaces, external secrets integration, and ingress prerequisites.
5. Document the promotion path from Helm values to Argo CD-managed releases.

## Branch sequence after `feature/gitops-bootstrap`

1. `feature/platform-observability-on-k8s` — wire Prometheus scraping, Grafana dashboards, and service monitor resources if the cluster supports them.
2. `feature/aws-runtime-foundation` — connect the chart assumptions to Terraform-managed AWS runtime infrastructure.
3. `feature/external-secrets-integration` — replace chart-managed secrets with cluster secret operators or cloud secret managers.
4. `feature/redis-runtime` — add Redis runtime wiring where platform services start consuming it in Kubernetes.

## Definition of done for `feature/gitops-bootstrap`

- Argo CD can reconcile the platform chart for at least one environment.
- Values overlays are consumable without manual manifest editing.
- Dependency ordering is explicit for namespace, secrets, dependencies, and services.
- Promotion and rollback steps are documented for the GitOps flow.

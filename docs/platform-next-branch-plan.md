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

`feature/external-secrets-integration`

## Completed by `feature/aws-runtime-foundation`

- Terraform modules (`infrastructure/terraform/modules/`): `vpc`, `eks`,
  `rds-postgres` (one database per microservice, matching
  `docker/postgres/init/01-create-databases.sql`), and `msk-kafka`.
- Per-environment root modules (`infrastructure/terraform/environments/{dev,prod}`)
  with separate state (S3 backend, configured via `backend.hcl`,
  not committed) and environment-appropriate sizing (dev: single NAT
  gateway, SPOT nodes, single-AZ RDS; prod: NAT per AZ, on-demand nodes,
  Multi-AZ RDS, deletion protection).
- EKS OIDC provider provisioned and exposed as an output, ready for IRSA
  trust policies (needed by `feature/external-secrets-integration`).
- `infrastructure/terraform/README.md` documents the Terraform vs.
  Helm/Argo CD split of responsibility and how outputs feed into
  `values-prod.yaml`'s external-infrastructure path
  (`postgresql.enabled=false` / `kafka.enabled=false`).
- Validated with `terraform fmt`, `terraform init`, and `terraform validate`
  for both environments.

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

## Exact scope for the next branch (`feature/external-secrets-integration`)

1. Install the External Secrets Operator into the cluster (via
   `gitops/bootstrap/shared`'s reserved `external-secrets` namespace).
2. Grant the operator's service account access to AWS Secrets
   Manager/SSM Parameter Store via IRSA, using the EKS OIDC provider
   output (`eks_oidc_provider_arn`) from `feature/aws-runtime-foundation`.
3. Replace the chart-managed `platform-secrets` Secret in prod with an
   `ExternalSecret` resource sourcing the same keys, keeping the dev path
   (`secrets.create=true`) unchanged.

## Branch sequence after `feature/external-secrets-integration`

1. `feature/redis-runtime` — add Redis runtime wiring where platform services start consuming it in Kubernetes, following the same `commonAnnotations` sync-wave pattern used for PostgreSQL/Kafka.

## Definition of done for `feature/aws-runtime-foundation`

- [x] VPC, EKS, RDS, and MSK are each defined as reusable Terraform modules.
- [x] Dev and prod environments have independent state and appropriately
      different sizing/HA defaults.
- [x] `terraform validate` passes for both environments.
- [x] The Terraform/Helm/Argo CD split of responsibility, and how Terraform
      outputs feed `values-prod.yaml`, is documented.

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

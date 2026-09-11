# Next Branch Plan

## Recommended next branch

`feature/helm-bootstrap`

## Exact scope for this branch

1. Finalize the first Helm chart skeleton for all current platform services.
2. Add deployable dependencies as chart-managed or documented external prerequisites for PostgreSQL, Kafka, and ingress.
3. Replace placeholder secrets and passwords in `values.yaml` with Kubernetes `Secret` references.
4. Split service configuration into environment overlays such as `values-dev.yaml` and `values-prod.yaml`.
5. Add chart templates for ConfigMap and Secret-driven runtime configuration where inline environment variables are no longer acceptable.
6. Add optional persistence and bootstrap wiring for local cluster bring-up.
7. Add a chart README with install, upgrade, and rollback commands.
8. Add GitHub Actions packaging and `helm lint` enforcement for pull requests.

## Branch sequence after `feature/helm-bootstrap`

1. `feature/helm-dependencies` — introduce PostgreSQL, Kafka, and prerequisite wiring strategy.
2. `feature/helm-secrets` — move credentials and JWT material into Kubernetes Secrets.
3. `feature/helm-environments` — add dev/prod values files and environment-specific ingress and replica settings.
4. `feature/gitops-bootstrap` — add Argo CD Application manifests or app-of-apps bootstrap structure.
5. `feature/platform-observability-on-k8s` — wire Prometheus scraping, Grafana dashboards, and service monitor resources if the cluster supports them.
6. `feature/aws-runtime-foundation` — connect the chart assumptions to Terraform-managed AWS runtime infrastructure.

## Definition of done for `feature/helm-bootstrap`

- `helm/enterprise-devops-platform` renders every current service in the platform.
- Gateway ingress is configurable and enabled by values.
- Service ports and baseline environment variables match the current Spring configuration.
- The branch leaves clear extension points for secrets, dependencies, and GitOps promotion instead of baking in production credentials.

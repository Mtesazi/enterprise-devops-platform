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

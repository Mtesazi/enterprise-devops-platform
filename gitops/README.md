# GitOps Bootstrap

This directory defines how Argo CD reconciles the `enterprise-devops-platform`
Helm chart (in `helm/enterprise-devops-platform`) across environments, using
this same repository as the single source of truth (app + config in one
repo, consumed by Argo CD via path-scoped `Application` sources).

## Repository layout

```
gitops/
  argocd/
    projects/
      platform-project.yaml     # AppProject scoping repos/destinations
    root/
      root-shared.yaml          # app-of-apps: cluster-wide prerequisites
      root-dev.yaml             # app-of-apps: dev environment
      root-prod.yaml            # app-of-apps: prod environment
    apps/
      shared/
        00-bootstrap-shared.yaml
      dev/
        00-bootstrap.yaml       # -> gitops/bootstrap/dev
        10-platform.yaml        # -> helm/enterprise-devops-platform + values-dev.yaml
      prod/
        00-bootstrap.yaml       # -> gitops/bootstrap/prod
        10-platform.yaml        # -> helm/enterprise-devops-platform + values-prod.yaml
  bootstrap/
    shared/                     # cluster-scoped prerequisites (one owner only)
      00-external-secrets-namespace.yaml
      01-ingress-prereqs.yaml
    dev/
      00-namespace.yaml         # enterprise-platform-dev namespace
    prod/
      00-namespace.yaml         # enterprise-platform-prod namespace
```

Environment-specific Helm values already live in the chart itself
(`values-dev.yaml`, `values-prod.yaml`); Argo CD `Application` resources
just select which file to layer on top of `values.yaml` per environment.
No values are duplicated into the `gitops/` tree.

## Sync-wave ordering

Ordering is expressed with `argocd.argoproj.io/sync-wave` annotations at two
levels:

**Across Applications** (app-of-apps, lower syncs first):
1. `platform-bootstrap-shared` (wave `-2`) — cluster-scoped prerequisites
   used by every environment.
2. `platform-bootstrap-{dev,prod}` (wave `-1`) — per-environment namespace.
3. `platform-{dev,prod}` (wave `0`) — the platform Helm chart release.

**Inside the platform chart** (annotated directly on chart templates and via
`commonAnnotations` on the Bitnami dependencies):
1. Wave `0` — `platform-secrets` Secret, and the `postgresql`/`kafka`
   dependency subcharts.
2. Wave `1` — platform service `Deployment`/`Service` resources.
3. Wave `2` — the gateway `Ingress`.

This guarantees namespaces and shared cluster prerequisites exist before the
chart syncs, and that secrets/dependencies are available before services
start, and services exist before the Ingress routes to them.

## Bootstrapping a cluster

Apply once, in order, against a cluster with Argo CD already installed:

```bash
kubectl apply -f gitops/argocd/projects/platform-project.yaml
kubectl apply -f gitops/argocd/root/root-shared.yaml
kubectl apply -f gitops/argocd/root/root-dev.yaml
# when ready for prod:
kubectl apply -f gitops/argocd/root/root-prod.yaml
```

Argo CD then reconciles everything else (bootstrap manifests, the platform
chart, and future child Applications added under `gitops/argocd/apps/`)
automatically.

## Promotion path: Helm values to Argo CD releases

1. Change is made to a service or to `helm/enterprise-devops-platform`
   (templates, `values.yaml`, or an environment overlay).
2. Merge to `main` (or the branch set as `targetRevision`).
3. `platform-dev` self-heals automatically (`syncPolicy.automated`) and picks
   up the change against `values-dev.yaml`.
4. Verify in dev: `argocd app get platform-dev`, check pod/service health,
   and confirm behavior.
5. Promote to prod by ensuring the same commit/tag is reachable at prod's
   `targetRevision`. `platform-prod` uses `values-prod.yaml`, so promotion is
   just "the same chart, different overlay" — no manifest duplication.
6. `platform-prod` has `prune: false` as a safety rail; review the diff with
   `argocd app diff platform-prod` before running `argocd app sync
   platform-prod` (or approving auto-sync) so pruning destructive changes in
   production is always a deliberate action.

## Rollback

- **Chart/values regression:** `argocd app rollback platform-dev
  <history-id>` (or `platform-prod`) to a previous synced revision, or revert
  the offending commit on `main` and let auto-sync/self-heal reconcile.
- **Bootstrap regression** (namespace/prereqs): revert the commit under
  `gitops/bootstrap/` the same way; `platform-bootstrap-shared` has
  `prune: false` so a bad shared-prerequisite change won't auto-delete
  cluster-scoped resources other environments depend on.
- **Full environment teardown:** delete the environment's root Application
  (`root-dev.yaml`/`root-prod.yaml`); the `resources-finalizer` ensures Argo
  CD cascades deletion of everything it created for that environment.

## Still expected in later branches

- `feature/platform-observability-on-k8s` — Prometheus/Grafana wiring.
- `feature/aws-runtime-foundation` — Terraform-managed AWS runtime.
- `feature/external-secrets-integration` — install the External Secrets
  Operator and replace `platform-secrets` with operator-managed secrets in
  the `external-secrets` namespace reserved here.
- `feature/redis-runtime` — Redis dependency wiring, following the same
  `commonAnnotations` sync-wave pattern used for PostgreSQL/Kafka.

# Terraform — AWS Runtime Foundation

Provisions the AWS infrastructure the `helm/enterprise-devops-platform` chart
assumes when `postgresql.enabled=false` / `kafka.enabled=false` (currently
the case in `values-prod.yaml`): an EKS cluster, an RDS Postgres instance
(one logical database per microservice), and an MSK Kafka cluster, all
inside a dedicated VPC.

## Split of responsibility: Terraform vs. Helm/Argo CD

| Layer | Owns | Tooling |
|---|---|---|
| **Terraform** (this directory) | Cloud infrastructure: VPC/subnets/NAT, EKS control plane + node groups, RDS Postgres instance + databases, MSK Kafka cluster, IAM roles (including the EKS OIDC provider used for IRSA) | `terraform apply` |
| **Helm chart** (`helm/enterprise-devops-platform`) | In-cluster workloads: service Deployments/Services, Ingress, Secrets, optional observability resources, and (in dev) the bundled `postgresql`/`kafka` Bitnami subcharts as a stand-in for the Terraform-managed equivalents | `helm upgrade --install` |
| **Argo CD** (`gitops/`) | Continuous reconciliation of the Helm chart against the cluster Terraform created | `argocd`/GitOps sync |

Terraform never touches Kubernetes objects, and Helm/Argo CD never touch AWS
resources — the only handoff is a handful of outputs (cluster name/endpoint,
RDS endpoint, MSK bootstrap brokers) that get turned into chart values or
Secret data, as described below. This keeps `values-dev.yaml` (bundled
subcharts, no Terraform required) and `values-prod.yaml` (external
infrastructure, Terraform required) on the same chart with no branching
logic in the templates themselves — only what the environment's values file
enables.

## Layout

```
infrastructure/terraform/
  modules/
    vpc/            # VPC, public/private subnets, IGW, NAT Gateway(s)
    eks/             # EKS cluster, managed node group, OIDC provider for IRSA
    rds-postgres/    # RDS instance + one database per microservice
    msk-kafka/       # MSK cluster (PLAINTEXT listener, matching current service config)
  environments/
    dev/             # smaller/cheaper sizing, single NAT gateway, SPOT nodes
    prod/            # multi-AZ NAT, on-demand nodes, Multi-AZ RDS, deletion protection
```

Each environment is a standalone root module with its own state (S3 backend,
configured out-of-band per `backend.hcl.example`) so dev and prod can never
share or clobber state.

## Prerequisites

- Terraform >= 1.5
- An S3 bucket + DynamoDB lock table for remote state (one pair per
  environment, or a shared bucket with per-environment keys — see
  `backend.hcl.example`)
- AWS credentials with permission to create the resources above

## Usage

```bash
cd infrastructure/terraform/environments/dev
cp backend.hcl.example backend.hcl        # fill in your state bucket/table; gitignored
cp terraform.tfvars.example terraform.tfvars  # fill in a real password; gitignored
terraform init -backend-config=backend.hcl
terraform plan
terraform apply
```

Repeat under `environments/prod` for the production environment (uses a
separate VPC CIDR, larger instance sizes, Multi-AZ RDS, and
`deletion_protection = true` by default).

After `apply`, wire the outputs into the deploy flow:

```bash
# point kubectl/helm/argocd at the new cluster
$(terraform output -raw kubeconfig_command)

# feed RDS/MSK endpoints into the chart's Secret and env vars for values-prod.yaml
terraform output rds_endpoint
terraform output kafka_bootstrap_brokers
```

Until `feature/external-secrets-integration` lands, the RDS master
credentials and per-service `SPRING_DATASOURCE_URL` host/port need to be
supplied to the chart the same way `platform-secrets` is today (out-of-band,
with `secrets.create=false` in `values-prod.yaml`). The EKS module's
OIDC provider output (`eks_oidc_provider_arn`) is what that later branch will
use to grant the External Secrets Operator's service account access to AWS
Secrets Manager/SSM via IRSA, without changing anything in this branch.

## What's intentionally out of scope here

- Kubernetes-level installs (ingress-nginx controller, AWS Load Balancer
  Controller, cert-manager, Prometheus Operator) — these are cluster
  add-ons, not infrastructure, and belong in `gitops/bootstrap/` or a future
  branch, not in Terraform.
- Secret externalization (`feature/external-secrets-integration`) and Redis
  (`feature/redis-runtime`) — both build directly on top of this
  foundation and are scoped as their own branches per
  `docs/platform-next-branch-plan.md`.

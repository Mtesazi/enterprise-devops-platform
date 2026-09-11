# enterprise-devops-platform Helm chart

Initial platform chart structure for the current microservice stack.

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

## Not yet bundled

- PostgreSQL
- Kafka
- Redis
- Argo CD resources
- environment-specific overlays
- Secret references for production credentials

## Example

```bash
helm install enterprise-platform ./helm/enterprise-devops-platform --namespace enterprise-platform --create-namespace
```

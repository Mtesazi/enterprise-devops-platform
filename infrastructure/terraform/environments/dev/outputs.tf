output "vpc_id" {
  value = module.vpc.vpc_id
}

output "eks_cluster_name" {
  value = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  value = module.eks.cluster_endpoint
}

output "eks_oidc_provider_arn" {
  value = module.eks.oidc_provider_arn
}

output "rds_endpoint" {
  value = module.rds.endpoint
}

output "rds_database_names" {
  value = module.rds.database_names
}

output "kafka_bootstrap_brokers" {
  value = module.msk.bootstrap_brokers
}

output "kubeconfig_command" {
  description = "Run this after apply to point kubectl/helm/argocd at the new cluster."
  value       = "aws eks update-kubeconfig --name ${module.eks.cluster_name} --region ${var.aws_region}"
}

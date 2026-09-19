output "cluster_name" {
  value = aws_eks_cluster.this.name
}

output "cluster_endpoint" {
  value = aws_eks_cluster.this.endpoint
}

output "cluster_certificate_authority_data" {
  value = aws_eks_cluster.this.certificate_authority[0].data
}

output "additional_cluster_security_group_id" {
  description = "Extra SG attached to the control plane ENIs; empty of rules by default, for operators to extend."
  value       = aws_security_group.cluster.id
}

output "cluster_security_group_id" {
  description = "EKS-managed primary security group, automatically shared with managed node group instances. Use this to grant node access to RDS/MSK."
  value       = aws_eks_cluster.this.vpc_config[0].cluster_security_group_id
}

output "node_role_arn" {
  value = aws_iam_role.node.arn
}

output "oidc_provider_arn" {
  description = "ARN of the cluster's OIDC provider, used to build IRSA trust policies (e.g. for external-secrets, aws-load-balancer-controller)."
  value       = aws_iam_openid_connect_provider.eks.arn
}

output "oidc_provider_url" {
  value = replace(aws_iam_openid_connect_provider.eks.url, "https://", "")
}

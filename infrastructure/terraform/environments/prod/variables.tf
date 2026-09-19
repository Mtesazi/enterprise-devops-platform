variable "aws_region" {
  type    = string
  default = "eu-west-1"
}

variable "environment" {
  type    = string
  default = "prod"
}

variable "environment_prefix" {
  description = "Short name used to prefix/name AWS resources for this environment."
  type        = string
  default     = "enterprise-platform-prod"
}

variable "cluster_name" {
  type    = string
  default = "enterprise-platform-prod"
}

variable "kubernetes_version" {
  type    = string
  default = "1.30"
}

variable "vpc_cidr" {
  type    = string
  default = "10.1.0.0/16"
}

variable "azs" {
  type    = list(string)
  default = ["eu-west-1a", "eu-west-1b", "eu-west-1c"]
}

variable "public_subnet_cidrs" {
  type    = list(string)
  default = ["10.1.0.0/24", "10.1.1.0/24", "10.1.2.0/24"]
}

variable "private_subnet_cidrs" {
  type    = list(string)
  default = ["10.1.10.0/24", "10.1.11.0/24", "10.1.12.0/24"]
}

variable "single_nat_gateway" {
  description = "Prod uses one NAT Gateway per AZ for availability; disabling saves cost but removes AZ isolation for egress traffic."
  type        = bool
  default     = false
}

variable "eks_endpoint_public_access" {
  description = "Disable once a bastion/VPN/CI runner path exists inside the VPC; keep true until then so the initial apply/deploy pipeline can reach the cluster."
  type        = bool
  default     = true
}

variable "node_instance_types" {
  type    = list(string)
  default = ["t3.large"]
}

variable "node_desired_size" {
  type    = number
  default = 3
}

variable "node_min_size" {
  type    = number
  default = 3
}

variable "node_max_size" {
  type    = number
  default = 6
}

variable "node_capacity_type" {
  type    = string
  default = "ON_DEMAND"
}

variable "rds_instance_class" {
  type    = string
  default = "db.r6g.large"
}

variable "rds_multi_az" {
  type    = bool
  default = true
}

variable "rds_deletion_protection" {
  type    = bool
  default = true
}

variable "rds_master_password" {
  description = "Master password for the prod RDS instance. Set via TF_VAR_rds_master_password or CI secret store; never commit a real value. Superseded once feature/external-secrets-integration lands."
  type        = string
  sensitive   = true
}

variable "msk_broker_count" {
  type    = number
  default = 3
}

variable "msk_instance_type" {
  type    = string
  default = "kafka.m5.large"
}

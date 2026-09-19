variable "aws_region" {
  type    = string
  default = "eu-west-1"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "environment_prefix" {
  description = "Short name used to prefix/name AWS resources for this environment."
  type        = string
  default     = "enterprise-platform-dev"
}

variable "cluster_name" {
  type    = string
  default = "enterprise-platform-dev"
}

variable "kubernetes_version" {
  type    = string
  default = "1.30"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "azs" {
  type    = list(string)
  default = ["eu-west-1a", "eu-west-1b"]
}

variable "public_subnet_cidrs" {
  type    = list(string)
  default = ["10.0.0.0/24", "10.0.1.0/24"]
}

variable "private_subnet_cidrs" {
  type    = list(string)
  default = ["10.0.10.0/24", "10.0.11.0/24"]
}

variable "single_nat_gateway" {
  type    = bool
  default = true
}

variable "eks_endpoint_public_access" {
  type    = bool
  default = true
}

variable "node_instance_types" {
  type    = list(string)
  default = ["t3.medium"]
}

variable "node_desired_size" {
  type    = number
  default = 2
}

variable "node_min_size" {
  type    = number
  default = 1
}

variable "node_max_size" {
  type    = number
  default = 3
}

variable "node_capacity_type" {
  type    = string
  default = "SPOT"
}

variable "rds_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "rds_multi_az" {
  type    = bool
  default = false
}

variable "rds_deletion_protection" {
  type    = bool
  default = false
}

variable "rds_master_password" {
  description = "Master password for the dev RDS instance. Set via TF_VAR_rds_master_password or a tfvars file that is NOT committed."
  type        = string
  sensitive   = true
}

variable "msk_broker_count" {
  type    = number
  default = 2
}

variable "msk_instance_type" {
  type    = string
  default = "kafka.t3.small"
}

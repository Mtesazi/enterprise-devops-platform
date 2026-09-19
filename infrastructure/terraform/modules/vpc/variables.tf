variable "name" {
  description = "Name prefix applied to all VPC resources (e.g. enterprise-platform-dev)."
  type        = string
}

variable "cidr_block" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "azs" {
  description = "Availability zones to spread subnets across."
  type        = list(string)
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets, one per AZ (ALB/NAT live here)."
  type        = list(string)
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets, one per AZ (EKS nodes, RDS, MSK live here)."
  type        = list(string)
}

variable "single_nat_gateway" {
  description = "Use a single shared NAT Gateway instead of one per AZ. Cheaper for dev, not recommended for prod."
  type        = bool
  default     = true
}

variable "cluster_name" {
  description = "EKS cluster name, used to tag subnets for cluster/ELB auto-discovery."
  type        = string
}

variable "tags" {
  description = "Common tags applied to all resources."
  type        = map(string)
  default     = {}
}

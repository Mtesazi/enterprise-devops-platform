variable "cluster_name" {
  description = "Name of the EKS cluster."
  type        = string
}

variable "kubernetes_version" {
  description = "Kubernetes version for the EKS control plane."
  type        = string
  default     = "1.30"
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  description = "Subnets for the control plane ENIs and managed node groups."
  type        = list(string)
}

variable "public_subnet_ids" {
  description = "Subnets for the control plane's public endpoint access, if enabled."
  type        = list(string)
}

variable "endpoint_public_access" {
  description = "Whether the EKS API server endpoint is reachable from outside the VPC. Disable for prod once a bastion/VPN/CI runner path exists inside the VPC."
  type        = bool
  default     = true
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
  default = 4
}

variable "node_capacity_type" {
  description = "ON_DEMAND or SPOT."
  type        = string
  default     = "ON_DEMAND"
}

variable "tags" {
  type    = map(string)
  default = {}
}

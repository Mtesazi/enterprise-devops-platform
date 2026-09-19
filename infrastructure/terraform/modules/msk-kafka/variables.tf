variable "cluster_name" {
  type = string
}

variable "kafka_version" {
  type    = string
  default = "3.7.x"
}

variable "number_of_broker_nodes" {
  description = "Must be a multiple of the number of subnets provided."
  type        = number
  default     = 2
}

variable "broker_instance_type" {
  type    = string
  default = "kafka.t3.small"
}

variable "broker_ebs_volume_size" {
  type    = number
  default = 50
}

variable "vpc_id" {
  type = string
}

variable "subnet_ids" {
  description = "Private subnets for MSK brokers, one AZ each."
  type        = list(string)
}

variable "allowed_security_group_ids" {
  description = "Security groups (e.g. EKS node group SG) allowed to reach the Kafka broker ports."
  type        = list(string)
  default     = []
}

variable "tags" {
  type    = map(string)
  default = {}
}

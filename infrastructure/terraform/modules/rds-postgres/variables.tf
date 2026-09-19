variable "identifier" {
  description = "RDS instance identifier."
  type        = string
}

variable "engine_version" {
  type    = string
  default = "16.4"
}

variable "instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "allocated_storage" {
  type    = number
  default = 20
}

variable "max_allocated_storage" {
  description = "Upper bound for RDS storage autoscaling."
  type        = number
  default     = 100
}

variable "multi_az" {
  type    = bool
  default = false
}

variable "database_names" {
  description = <<-EOT
    Databases to provision on the instance, one per microservice, matching
    docker/postgres/init/01-create-databases.sql plus payroll_db so every
    service has a dedicated logical database in every environment.
  EOT
  type        = list(string)
  default     = ["auth_db", "employee_db", "department_db", "notification_db", "audit_db", "payroll_db"]
}

variable "master_username" {
  type    = string
  default = "postgres"
}

variable "master_password" {
  description = "Master password for the RDS instance. Pass via a secret store / CI secret, never commit a real value."
  type        = string
  sensitive   = true
}

variable "vpc_id" {
  type = string
}

variable "subnet_ids" {
  description = "Private subnets for the DB subnet group."
  type        = list(string)
}

variable "allowed_security_group_ids" {
  description = "Security groups (e.g. EKS node group SG) allowed to reach Postgres on port 5432."
  type        = list(string)
  default     = []
}

variable "backup_retention_period" {
  type    = number
  default = 7
}

variable "deletion_protection" {
  type    = bool
  default = false
}

variable "skip_final_snapshot" {
  type    = bool
  default = true
}

variable "tags" {
  type    = map(string)
  default = {}
}

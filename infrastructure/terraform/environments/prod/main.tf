terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.60"
    }
    postgresql = {
      source  = "cyrilgdn/postgresql"
      version = "~> 1.22"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  # State is intentionally left as local backend in source control. Configure
  # a remote backend (S3 + DynamoDB lock table) per environment via
  # `terraform init -backend-config=backend.hcl`, using a backend.hcl file
  # that is NOT committed (see backend.hcl.example).
  backend "s3" {}
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}

locals {
  common_tags = {
    Project     = "enterprise-devops-platform"
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

module "vpc" {
  source = "../../modules/vpc"

  name                 = "${var.environment_prefix}-vpc"
  cidr_block           = var.vpc_cidr
  azs                  = var.azs
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  single_nat_gateway   = var.single_nat_gateway
  cluster_name         = var.cluster_name
  tags                 = local.common_tags
}

module "eks" {
  source = "../../modules/eks"

  cluster_name           = var.cluster_name
  kubernetes_version     = var.kubernetes_version
  vpc_id                 = module.vpc.vpc_id
  private_subnet_ids     = module.vpc.private_subnet_ids
  public_subnet_ids      = module.vpc.public_subnet_ids
  endpoint_public_access = var.eks_endpoint_public_access

  node_instance_types = var.node_instance_types
  node_desired_size   = var.node_desired_size
  node_min_size       = var.node_min_size
  node_max_size       = var.node_max_size
  node_capacity_type  = var.node_capacity_type

  tags = local.common_tags
}

module "rds" {
  source = "../../modules/rds-postgres"

  identifier      = "${var.environment_prefix}-postgres"
  instance_class  = var.rds_instance_class
  multi_az        = var.rds_multi_az
  master_password = var.rds_master_password

  vpc_id                     = module.vpc.vpc_id
  subnet_ids                 = module.vpc.private_subnet_ids
  allowed_security_group_ids = [module.eks.cluster_security_group_id]

  deletion_protection = var.rds_deletion_protection
  skip_final_snapshot = !var.rds_deletion_protection

  tags = local.common_tags
}

module "msk" {
  source = "../../modules/msk-kafka"

  cluster_name           = "${var.environment_prefix}-kafka"
  number_of_broker_nodes = var.msk_broker_count
  broker_instance_type   = var.msk_instance_type

  vpc_id                     = module.vpc.vpc_id
  subnet_ids                 = module.vpc.private_subnet_ids
  allowed_security_group_ids = [module.eks.cluster_security_group_id]

  tags = local.common_tags
}

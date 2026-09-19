output "vpc_id" {
  description = "ID of the VPC."
  value       = aws_vpc.this.id
}

output "vpc_cidr_block" {
  value = aws_vpc.this.cidr_block
}

output "public_subnet_ids" {
  description = "Subnet IDs suitable for internet-facing load balancers."
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Subnet IDs suitable for EKS nodes, RDS, and MSK."
  value       = aws_subnet.private[*].id
}

output "azs" {
  value = var.azs
}

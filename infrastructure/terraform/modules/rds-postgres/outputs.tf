output "endpoint" {
  value = aws_db_instance.this.endpoint
}

output "address" {
  value = aws_db_instance.this.address
}

output "port" {
  value = aws_db_instance.this.port
}

output "database_names" {
  value = var.database_names
}

output "security_group_id" {
  value = aws_security_group.this.id
}

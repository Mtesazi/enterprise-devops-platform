output "bootstrap_brokers" {
  description = "Plaintext bootstrap broker connection string for spring.kafka.bootstrap-servers."
  value       = aws_msk_cluster.this.bootstrap_brokers
}

output "zookeeper_connect_string" {
  value = aws_msk_cluster.this.zookeeper_connect_string
}

output "security_group_id" {
  value = aws_security_group.this.id
}

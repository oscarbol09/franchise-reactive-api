output "rds_endpoint" {
  description = "Connection endpoint for the RDS MySQL instance"
  value       = aws_db_instance.mysql.endpoint
}

output "rds_secret_arn" {
  description = "ARN of the Secrets Manager secret holding RDS credentials"
  value       = aws_db_instance.mysql.master_user_secret[0].secret_arn
}

output "ecr_repository_url" {
  description = "ECR repository URL for pushing the Docker container image"
  value       = aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  description = "ECS Fargate cluster name"
  value       = aws_ecs_cluster.main.name
}

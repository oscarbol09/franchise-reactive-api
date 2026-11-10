variable "aws_region" {
  description = "AWS region for provisioning resources"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project identifier prefix"
  type        = string
  default     = "franchise-api"
}

variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
  default     = "prod"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be one of: dev, staging, prod."
  }
}

variable "vpc_cidr" {
  description = "CIDR block for the isolated VPC"
  type        = string
  default     = "10.20.0.0/16"
}

variable "db_instance_class" {
  description = "RDS MySQL instance type"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_name" {
  description = "Database schema name"
  type        = string
  default     = "franchise_db"
}

variable "db_username" {
  description = "Master username for RDS MySQL"
  type        = string
  default     = "franchise_admin"
}

variable "container_image_tag" {
  description = "Immutable container image tag deployed to ECS Fargate"
  type        = string
  default     = "1.0.0"
}

variable "ecs_task_cpu" {
  description = "Fargate task CPU units (256 = 0.25 vCPU)"
  type        = number
  default     = 512
}

variable "ecs_task_memory" {
  description = "Fargate task memory in MiB"
  type        = number
  default     = 1024
}

variable "ecs_desired_count" {
  description = "Desired number of running ECS Fargate tasks"
  type        = number
  default     = 2
}

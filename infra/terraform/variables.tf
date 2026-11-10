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

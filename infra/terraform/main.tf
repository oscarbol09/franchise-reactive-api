data "aws_availability_zones" "available" {
  state = "available"
}

# ==============================================================================
# 1. Network Isolation (VPC & Private Database Subnets)
# ==============================================================================
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.project_name}-${var.environment}-vpc"
  }
}

resource "aws_subnet" "private_db_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, 10)
  availability_zone = data.aws_availability_zones.available.names[0]

  tags = {
    Name = "${var.project_name}-${var.environment}-db-subnet-a"
  }
}

resource "aws_subnet" "private_db_b" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, 11)
  availability_zone = data.aws_availability_zones.available.names[1]

  tags = {
    Name = "${var.project_name}-${var.environment}-db-subnet-b"
  }
}

resource "aws_db_subnet_group" "rds" {
  name       = "${var.project_name}-${var.environment}-db-subnet-group"
  subnet_ids = [aws_subnet.private_db_a.id, aws_subnet.private_db_b.id]
}

# ==============================================================================
# 2. Security Groups (Least Privilege Ingress)
# ==============================================================================
resource "aws_security_group" "ecs_tasks" {
  name        = "${var.project_name}-${var.environment}-ecs-sg"
  description = "Security group for Franchise API ECS Fargate tasks"
  vpc_id      = aws_vpc.main.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "rds_mysql" {
  name        = "${var.project_name}-${var.environment}-rds-sg"
  description = "Allow MySQL 3306 traffic strictly from ECS tasks security group"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "MySQL access from ECS Fargate tasks only"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }
}

# ==============================================================================
# 3. Persistence Layer: Amazon RDS MySQL 8.4 (AWS Managed Master Password)
# ==============================================================================
resource "aws_db_instance" "mysql" {
  identifier                  = "${var.project_name}-${var.environment}-mysql"
  engine                      = "mysql"
  engine_version              = "8.4.3"
  instance_class              = var.db_instance_class
  allocated_storage           = 20
  max_allocated_storage       = 100
  storage_type                = "gp3"
  storage_encrypted           = true
  db_name                     = var.db_name
  username                    = var.db_username
  manage_master_user_password = true

  db_subnet_group_name   = aws_db_subnet_group.rds.name
  vpc_security_group_ids = [aws_security_group.rds_mysql.id]
  multi_az               = var.environment == "prod"
  publicly_accessible    = false
  backup_retention_period = 7
  deletion_protection    = var.environment == "prod"
  skip_final_snapshot    = var.environment != "prod"

  lifecycle {
    prevent_destroy = false
  }
}

# ==============================================================================
# 4. Container Registry (ECR) & ECS Cluster
# ==============================================================================
resource "aws_ecr_repository" "app" {
  name                 = "${var.project_name}-${var.environment}"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-${var.environment}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

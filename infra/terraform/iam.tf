# ============================================================
#  IAM rolleri - ECS Fargate icin iki rol
# ============================================================

# Ortak "trust policy": bu rolleri sadece ECS'in gorev servisi
# (ecs-tasks.amazonaws.com) ustlenebilir. Iki rolde de ayni oldugu icin
# 'locals' ile tek yerde tanimlayip tekrar kullaniyoruz.
locals {
  ecs_assume_role = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

# ------------------------------------------------------------
# 1) TASK ROLE: calisan uygulamanin kimligi.
#    query-api bununla DynamoDB okur / SQS yoklar.
# ------------------------------------------------------------
resource "aws_iam_role" "task" {
  name               = "arac-bakim-tf-task-role"
  assume_role_policy = local.ecs_assume_role
}

resource "aws_iam_role_policy_attachment" "task_dynamodb" {
  role       = aws_iam_role.task.name # <- referans: once bu rol olusur (bagimlilik)
  policy_arn = "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
}

resource "aws_iam_role_policy_attachment" "task_sqs" {
  role       = aws_iam_role.task.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSQSFullAccess"
}

# ------------------------------------------------------------
# 2) EXECUTION ROLE: ECS PLATFORMUNUN kimligi.
#    Image'i ECR'den ceker + loglari CloudWatch'a yazar.
# ------------------------------------------------------------
resource "aws_iam_role" "execution" {
  name               = "arac-bakim-tf-exec-role"
  assume_role_policy = local.ecs_assume_role
}

resource "aws_iam_role_policy_attachment" "execution_managed" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

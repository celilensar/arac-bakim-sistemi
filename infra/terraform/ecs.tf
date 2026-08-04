# ============================================================
#  ECS - cluster, log grubu, task definition
# ============================================================

# 1) CloudWatch log grubu: konteynerin loglari buraya akar.
#    (ECS Express'te bu otomatikti; simdi biz olusturuyoruz.)
resource "aws_cloudwatch_log_group" "query_api" {
  name              = "/ecs/arac-bakim-tf/query-api"
  retention_in_days = 7 # loglari 7 gun sakla (maliyet kontrolu)
}

# 2) ECS cluster: gorevlerin (task) calisacagi mantiksal grup.
resource "aws_ecs_cluster" "main" {
  name = "arac-bakim-tf-cluster"
}

# 3) Task definition: "bir konteyner nasil calisir" tarifi/sablonu.
#    Bu tek basina bir sey CALISTIRMAZ (ucret yok); service onu kullanacak.
resource "aws_ecs_task_definition" "query_api" {
  family                   = "arac-bakim-tf-query-api"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc" # Fargate icin zorunlu
  cpu                      = "1024"   # 1 vCPU
  memory                   = "2048"   # 2 GB
  execution_role_arn       = aws_iam_role.execution.arn # image cek + log yaz
  task_role_arn            = aws_iam_role.task.arn       # uygulamanin DynamoDB/SQS kimligi

  # Konteyner(ler)in tanimi. jsonencode ile HCL nesnesini JSON'a ceviriyoruz.
  container_definitions = jsonencode([
    {
      name      = "query-api"
      image     = var.image_uri
      essential = true

      # 8080'i disa ac (ALB buraya baglanacak)
      portMappings = [
        { containerPort = 8080, protocol = "tcp" }
      ]

      # KRITIK: gecen sefer unutup 503 aldigimiz env burada koda gomulu.
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "aws" }
      ]

      # Loglari CloudWatch'a yonlendir
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.query_api.name
          "awslogs-region"        = var.region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

# ============================================================
#  ECS Service - her seyi birbirine baglar, task'i calistirir
# ============================================================
# Service = "su task definition'dan, su kadar kopya (desired_count),
# su agda, su ALB'ye bagli olarak SUREKLI calissin" der.
# Task olurse yenisini baslatir (self-healing).

resource "aws_ecs_service" "query_api" {
  name            = "arac-bakim-tf-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.query_api.arn
  desired_count   = 1 # 1 kopya calissin
  launch_type     = "FARGATE"

  # Ag ayari: task hangi subnet + guvenlik grubunda calisacak
  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.task.id]
    assign_public_ip = true # ECR'den image cekmek + DynamoDB/SQS'e cikmak icin (NAT yok)
  }

  # Load balancer baglantisi: bu task'i target group'a kaydet
  load_balancer {
    target_group_arn = aws_lb_target_group.query_api.arn
    container_name   = "query-api" # task def'teki konteyner adiyla ayni
    container_port   = 8080
  }

  # Listener hazir olmadan service'i baslatma (bagimlilik netlestirme)
  depends_on = [aws_lb_listener.http]
}

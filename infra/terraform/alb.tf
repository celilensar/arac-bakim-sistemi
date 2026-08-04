# ============================================================
#  Ag + ALB (Application Load Balancer) + guvenlik gruplari
# ============================================================

# --- Hazir ag: yeni VPC kurmak yerine hesabin VARSAYILAN VPC'sini kullan ---
# data = var olani OKU. Boylece subnet/VPC'yi elle yazmiyoruz.
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# --- Guvenlik grubu 1: ALB (internete acik) ---
# Sadece 80 portundan giris; her yere cikis.
resource "aws_security_group" "alb" {
  name        = "arac-bakim-tf-alb-sg"
  description = "ALB: internetten HTTP 80"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "Internetten 80"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1" # tum protokoller
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# --- Guvenlik grubu 2: Fargate task ---
# 8080'e SADECE ALB'den giris (internete kapali). Iyi guvenlik pratigi.
resource "aws_security_group" "task" {
  name        = "arac-bakim-tf-task-sg"
  description = "Fargate task: sadece ALB uzerinden 8080"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "Sadece ALB SG uzerinden 8080"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id] # <- IP degil, dogrudan ALB'nin SG'si
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"] # DynamoDB/SQS'e cikabilsin
  }
}

# --- ALB'nin kendisi ---
resource "aws_lb" "main" {
  name               = "arac-bakim-tf-alb"
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = data.aws_subnets.default.ids # varsayilan subnet'lere yayilir
}

# --- Target group: ALB isteklleri "nereye" gonderecek ---
# Fargate awsvpc modunda hedef IP'dir (target_type = ip).
resource "aws_lb_target_group" "query_api" {
  name        = "arac-bakim-tf-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default.id
  target_type = "ip"

  health_check {
    path                = "/api/fleet" # 200 donen endpoint (kok / degil!)
    matcher             = "200"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

# --- Listener: ALB'nin 80 portu -> target group'a yonlendir ---
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.query_api.arn
  }
}

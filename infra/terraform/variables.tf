# ============================================================
#  Degiskenler - tekrar kullanilan / disaridan verilebilen degerler
# ============================================================
# 'default' oldugu icin ekstra bir sey girmene gerek yok; istersen
# 'terraform apply -var="image_uri=..."' ile de gecebilirsin.

variable "region" {
  description = "AWS bolgesi"
  type        = string
  default     = "eu-central-1"
}

variable "image_uri" {
  description = "query-api ECR image adresi"
  type        = string
  default     = "508564776181.dkr.ecr.eu-central-1.amazonaws.com/query-api:latest"
}

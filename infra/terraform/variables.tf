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
  description = "query-api ECR image adresi. Kendi hesabina gore terraform.tfvars ile ver: <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/query-api:latest"
  type        = string
  default     = "YOUR_ACCOUNT_ID.dkr.ecr.eu-central-1.amazonaws.com/query-api:latest"
}

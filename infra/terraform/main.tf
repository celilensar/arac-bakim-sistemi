# ============================================================
#  Terraform ana yapilandirma - ilk adim (ogrenme)
# ============================================================

# 1) terraform blogu: Terraform'un kendi ayarlari.
#    Hangi provider'lari (bulut eklentileri) kullanacagimizi belirtir.
terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws" # AWS resmi provider'i
      version = "~> 5.0"         # 5.x surumu (5.0 <= v < 6.0)
    }
  }
}

# 2) provider blogu: AWS'ye "hangi bolgede calisacagim" der.
#    Kimlik bilgisi burada YOK -> senin 'aws configure' ayarlarindan
#    (~/.aws) otomatik gelir. (Sifre koda yazilmaz kurali burada da gecerli.)
provider "aws" {
  region = var.region # <- artik degiskenden geliyor (variables.tf)
}

# 3) data source: AWS'de VAR OLAN bir seyi OKUR (olusturmaz).
#    aws_caller_identity = "ben kimim?" -> hesap ID doner.
#    Ilk apply'in guvenli olsun diye sadece okuma yapiyoruz.
data "aws_caller_identity" "current" {}

# 4) output: apply sonunda ekrana bir deger yazdirir.
output "hesap_id" {
  value = data.aws_caller_identity.current.account_id
}

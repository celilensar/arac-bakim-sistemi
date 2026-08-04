# ============================================================
#  Output'lar - apply sonunda ekrana yazilan onemli degerler
# ============================================================

output "alb_url" {
  description = "ALB uzerinden uygulama adresi"
  value       = "http://${aws_lb.main.dns_name}"
}

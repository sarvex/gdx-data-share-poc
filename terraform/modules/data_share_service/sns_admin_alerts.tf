module "sns_admin_alerts" {
  source = "../sns"

  account_id          = data.aws_caller_identity.current.account_id
  environment         = var.environment
  name                = "gdx-admin-alerts"
  notification_emails = ["gdx-dev-team@digital.cabinet-office.gov.uk"]
}

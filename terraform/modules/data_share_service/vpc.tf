module "vpc" {
  source = "../vpc"

  environment   = var.environment
  account_id    = data.aws_caller_identity.current.account_id
  region        = var.region
  name_prefix   = "${var.environment}-gdx-data-share-poc-"
  vpc_cidr      = var.vpc_cidr
  sns_topic_arn = module.sns.topic_arn

  depends_on = [module.sns]
}

locals {
  identifier    = "events"
  scope_publish = "publish"
  scope_consume = "consume"
  scope_admin   = "admin"
}

resource "aws_cognito_user_pool" "pool" {
  name = "${var.environment}-gdx-data-share"
}

resource "aws_cognito_user_pool_domain" "domain" {
  domain       = "${var.environment}-gdx-data-share"
  user_pool_id = aws_cognito_user_pool.pool.id
}

resource "aws_cognito_resource_server" "events" {
  identifier = local.identifier
  name       = "GDX Events"

  user_pool_id = aws_cognito_user_pool.pool.id

  scope {
    scope_name        = local.scope_publish
    scope_description = "Can publish events with GDX"
  }

  scope {
    scope_name        = local.scope_consume
    scope_description = "Can consume events from GDX"
  }

  scope {
    scope_name        = local.scope_admin
    scope_description = "Can manage events and users of GDX"
  }
}

resource "aws_cognito_user_pool_client" "admin" {
  name                                 = "${var.environment}-admin"
  user_pool_id                         = aws_cognito_user_pool.pool.id
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["implicit"]
  allowed_oauth_scopes                 = aws_cognito_resource_server.events.scope_identifiers
  generate_secret                      = true
  explicit_auth_flows                  = ["ADMIN_NO_SRP_AUTH"]
  callback_urls                        = [var.callback_url]
  supported_identity_providers         = ["COGNITO"]
}

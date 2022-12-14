resource "aws_codedeploy_app" "gdx_data_share_poc" {
  compute_platform = "ECS"
  name             = "${var.environment}-gdx-data-share-poc"
}

resource "aws_codedeploy_deployment_group" "gdx_data_share_poc" {
  app_name               = aws_codedeploy_app.gdx_data_share_poc.name
  deployment_group_name  = aws_codedeploy_app.gdx_data_share_poc.name
  service_role_arn       = aws_iam_role.ecs_codedeploy.arn
  deployment_config_name = "CodeDeployDefault.ECSAllAtOnce"

  auto_rollback_configuration {
    enabled = true
    events  = ["DEPLOYMENT_FAILURE"]
  }

  blue_green_deployment_config {
    deployment_ready_option {
      action_on_timeout = "CONTINUE_DEPLOYMENT"
    }
    terminate_blue_instances_on_deployment_success {
      action                           = "TERMINATE"
      termination_wait_time_in_minutes = 0
    }
  }

  deployment_style {
    deployment_option = "WITH_TRAFFIC_CONTROL"
    deployment_type   = "BLUE_GREEN"
  }

  ecs_service {
    cluster_name = aws_ecs_cluster.main.name
    service_name = aws_ecs_service.gdx_data_share_poc.name
  }

  load_balancer_info {

    target_group_pair_info {

      prod_traffic_route {
        listener_arns = [aws_lb_listener.listener-http.arn]
      }

      target_group {
        name = aws_lb_target_group.green.name
      }

      target_group {
        name = aws_lb_target_group.blue.name
      }
    }
  }

  depends_on = [
    aws_ecs_service.gdx_data_share_poc
  ]
}

resource "aws_iam_role" "ecs_codedeploy" {
  name = "${var.environment}-ecs-code-deploy-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = "sts:AssumeRole"
        Principal = {
          Service = "codedeploy.amazonaws.com"
        },
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "AWSCodeDeployRoleForECS" {
  policy_arn = "arn:aws:iam::aws:policy/AWSCodeDeployRoleForECS"
  role       = aws_iam_role.ecs_codedeploy.name
}

data "aws_iam_policy_document" "passrole_codedeploy" {
  statement {
    effect = "Allow"
    actions = [
      "iam:PassRole",
    ]
    resources = [
      aws_iam_role.ecs_execution.arn,
      aws_iam_role.ecs_task.arn,
    ]
  }
}

resource "aws_iam_policy" "passrole_codedeploy" {
  name   = "${var.environment}-passrole-ecstaskexecution"
  policy = data.aws_iam_policy_document.passrole_codedeploy.json
}

resource "aws_iam_role_policy_attachment" "passrole_codedeploy" {
  policy_arn = aws_iam_policy.passrole_codedeploy.arn
  role       = aws_iam_role.ecs_codedeploy.name
}
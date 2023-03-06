# We want the load balancer available to our cloudfront distribution, it is locked down from the wider internet with
# security group rules
#tfsec:ignore:aws-elb-alb-not-public
resource "aws_lb" "load_balancer" {
  name                       = "acquirer-ui-lb"
  load_balancer_type         = "application"
  subnets                    = var.public_subnet_ids
  security_groups            = [aws_security_group.lb.id]
  drop_invalid_header_fields = true
}

#tfsec:ignore:aws-elb-http-not-used
resource "aws_lb_listener" "listener_http" {
  load_balancer_arn = aws_lb.load_balancer.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.acquirer_ui.arn
  }

  lifecycle {
    ignore_changes = [default_action]
  }

  depends_on = [aws_lb_target_group.acquirer_ui]
}

resource "aws_lb_target_group" "acquirer_ui" {
  name        = "acquirer-ui-target"
  port        = 3000
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = var.vpc_id

  health_check {
    path = "/api/health"
  }

  lifecycle {
    create_before_destroy = true
  }
}
resource "aws_security_group" "lb" {
  name        = "acquirer-ui-lb"
  description = "Allow access to Acquirer UI LB from Cloudfront"
  vpc_id      = var.vpc_id

  lifecycle {
    create_before_destroy = true
  }
}

data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

resource "aws_security_group_rule" "lb_cloudfront" {
  type              = "ingress"
  protocol          = "tcp"
  from_port         = 80
  to_port           = 80
  description       = "LB ingress rule for cloudfront"
  security_group_id = aws_security_group.lb.id
  prefix_list_ids   = [data.aws_ec2_managed_prefix_list.cloudfront.id]
}

resource "aws_s3_bucket" "cloudfront_site_bucket" {
  bucket = "acquirer-ui-site"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_public_access_block" "cloudfront_site_bucket_public_access" {
  bucket                  = aws_s3_bucket.cloudfront_site_bucket.id
  block_public_acls       = true
  block_public_policy     = true
  restrict_public_buckets = true
  ignore_public_acls      = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cloudfront_site_bucket_encryption" {
  bucket = aws_s3_bucket.cloudfront_site_bucket.bucket

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.log_key.arn
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_acl" "cloudfront_site_bucket_grants" {
  bucket = aws_s3_bucket.cloudfront_site_bucket.id
  access_control_policy {
    grant {
      grantee {
        id   = data.aws_canonical_user_id.current.id
        type = "CanonicalUser"
      }
      permission = "FULL_CONTROL"
    }

    grant {
      grantee {
        id   = "c4c1ede66af53448b93c283ce9448c4ba468c9432aa01d700d3878632f77d2d0" # The Canonical ID for Cloudfront
        type = "CanonicalUser"
      }
      permission = "FULL_CONTROL"
    }

    owner {
      id = data.aws_canonical_user_id.current.id
    }
  }
}

resource "aws_s3_bucket_website_configuration" "cloudfront_site" {
  bucket = aws_s3_bucket.cloudfront_site_bucket.id

  index_document {
    suffix = "index.html"
  }
}

resource "aws_s3_object" "html" {
  for_each = fileset("../../../frontend/", "**/*.html")

  bucket       = aws_s3_bucket.cloudfront_site_bucket.bucket
  key          = each.value
  source       = "../../../frontend/${each.value}"
  etag         = filemd5("../../../frontend/${each.value}")
  content_type = "text/html"
}

resource "aws_s3_object" "woff" {
  for_each = fileset("../../../frontend/", "**/*.woff")

  bucket       = aws_s3_bucket.cloudfront_site_bucket.bucket
  key          = each.value
  source       = "../../../frontend/${each.value}"
  etag         = filemd5("../../../frontend/${each.value}")
  content_type = "font/woff"
}

resource "aws_s3_object" "woff2" {
  for_each = fileset("../../../frontend/", "**/*.woff2")

  bucket       = aws_s3_bucket.cloudfront_site_bucket.bucket
  key          = each.value
  source       = "../../../frontend/${each.value}"
  etag         = filemd5("../../../frontend/${each.value}")
  content_type = "font/woff2"
}
resource "aws_s3_object" "icons" {
  for_each = fileset("../../../frontend/", "**/*.ico")

  bucket       = aws_s3_bucket.cloudfront_site_bucket.bucket
  key          = each.value
  source       = "../../../frontend/${each.value}"
  etag         = filemd5("../../../frontend/${each.value}")
  content_type = "image/x-icon"
}

resource "aws_s3_object" "images" {
  for_each = fileset("../../../frontend/", "**/*.png")

  bucket       = aws_s3_bucket.cloudfront_site_bucket.bucket
  key          = each.value
  source       = "../../../frontend/${each.value}"
  etag         = filemd5("../../../frontend/${each.value}")
  content_type = "image/png"
}

resource "aws_s3_object" "images" {
  for_each = fileset("../../../frontend/", "**/*.png")

  bucket       = aws_s3_bucket.cloudfront_site_bucket.bucket
  key          = each.value
  source       = "../../../frontend/${each.value}"
  etag         = filemd5("../../../frontend/${each.value}")
  content_type = "image/png"
}

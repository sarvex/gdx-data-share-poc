# Code cloned from https://github.com/schubergphilis/terraform-aws-mcaf-securityhub-findings-manager/

import boto3
import os
import yaml

table_name = "securityhub-suppression-list"

with open("suppressions.yml") as f:
    suppressions = yaml.safe_load(f.read())

dynamodb_client = boto3.client("dynamodb")

# Upsert existing items
for suppression_id in suppressions.get("Suppressions", {}):
    dynamodb_client.put_item(
        TableName=table_name,
        Item={
            "title": {
                "S": suppression_id
            },
            "data": {
                "L": [
                    {
                        "M": {
                            "resources": {"SS": item["resources"]},
                            "notes": {"S": item["notes"]}
                        }
                    }
                    for item in suppressions["Suppressions"][suppression_id]
                ]
            }
        }
    )
    print(f"Upserted rule with title {suppression_id} in suppression table.")

# Remove rules no longer in the suppressions config
scan_paginator = dynamodb_client.get_paginator("scan")
scan_pages = scan_paginator.paginate(
    TableName=table_name
)

for scan_page in scan_pages:
    for item in scan_page.get("Items", []):
        if item["title"]["S"] not in suppressions.get("Suppressions", {}).keys():
            dynamodb_client.delete_item(
                TableName=table_name,
                Key={
                    "title": {"S": item["title"]["S"]}
                }
            )
            print(f'Removed rule with title {item["title"]["S"]} from suppression table.')

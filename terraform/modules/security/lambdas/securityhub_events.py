import os
from dataclasses import dataclass
from datetime import datetime
from re import search

import boto3
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.data_classes import EventBridgeEvent

logger = Logger()
DYNAMODB_TABLE_NAME = os.environ['DYNAMODB_TABLE_NAME']
YAML_CONFIGURATION_FILE = 'suppressor.yml'

securityhub = boto3.client('securityhub')
table = boto3.resource('dynamodb').Table(name=DYNAMODB_TABLE_NAME)


@dataclass
class SuppressionRule:
    resources: [str]
    notes: str


def suppress_in_securityhub(finding_id, product_arn, notes):
    now = datetime.now()

    securityhub.batch_update_findings(
        FindingIdentifiers=[{
            'Id': finding_id,
            'ProductArn': product_arn
        }],
        Workflow={'Status': 'SUPPRESSED'},
        Note={
            'Text': f'{notes} - Suppressed by the Security Hub Suppressor at {now.strftime("%Y-%m-%d %H:%M:%S")}',
            'UpdatedBy': 'suppression-lambda'
        }
    )


def suppress_finding(finding):
    title = finding['Title']
    product_arn = finding['ProductArn']
    finding_id = finding['Id']

    suppression_rules = list(map(
        lambda table_item: SuppressionRule(resources=table_item['resources'], notes=table_item['notes']),
        table.get_item(Key={"title": title}).get('Item', {}).get('data', {})
    ))

    if not suppression_rules:
        logger.warning(f'No rules found for title: {title}')
        return None

    for resource in finding.get('Resources', []):
        resource_id = resource['Id']
        for rule in suppression_rules:
            for resource_check in rule.resources:
                if match := search(resource_check, resource_id):
                    logger.info(
                        f'Perform Suppression on finding {finding_id}, '
                        f'matched resource check: {resource_check}, '
                        f'with title: {title}'
                    )
                    suppress_in_securityhub(finding_id, product_arn, rule.notes)
                    return finding_id
    return None


def suppress(event: EventBridgeEvent) -> []:
    findings = event['detail'].get('findings', [])
    suppressed_finding_ids = []
    for finding in findings:
        finding_id = finding['Id']
        if finding_id in suppressed_finding_ids:
            continue
        if suppress_finding(finding):
            suppressed_finding_ids.append(finding_id)
    return suppressed_finding_ids


def handle_event_bridge_event(event: EventBridgeEvent):
    suppressed_finding_ids = suppress(event)
    if len(suppressed_finding_ids) > 0:
        logger.info(f'Total findings processed: {len(suppressed_finding_ids)}')
        return {
            'finding_state': 'suppressed'
        }
    return {
        'finding_state': 'skipped'
    }

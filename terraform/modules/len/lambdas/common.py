import json
from urllib import request, parse


def get_auth_token(auth_url, client_id, client_secret):
    auth_request_data = parse.urlencode({
        "grant_type": "client_credentials",
        "client_id": client_id,
        "client_secret": client_secret
    }).encode()
    auth_request = request.Request(
        url=auth_url,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
        data=auth_request_data
    )
    return json.loads(request.urlopen(auth_request).read())["access_token"]


def record_metric(client, namespace, metric_name, value, unit="Count"):
    client.put_metric_data(Namespace=namespace, MetricData=[
        {
            "MetricName": metric_name,
            "Value": value,
            "Unit": unit
        }
    ])

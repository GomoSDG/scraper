import boto3
import json

sqs = boto3.client('sqs')

def send_message(message):
    sqs.send_message(
        QueueUrl='https://sqs.us-east-1.amazonaws.com/613493892787/cleaned-prices',
        MessageBody=json.dumps(message)
    )
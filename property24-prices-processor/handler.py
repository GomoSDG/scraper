import json
import base64
import pytesseract
import io
from PIL import Image
import sqs
import numpy as np
import re
import os

if os.getenv('AWS_EXECUTION_ENV') is not None:
    os.environ['LD_LIBRARY_PATH'] = '/opt/lib'
    os.environ['TESSDATA_PREFIX'] = '/opt/tesseract/share/tessdata'
    pytesseract.pytesseract.tesseract_cmd = '/opt/bin/tesseract'

def sqs_message_to_dict(record):
    return json.loads(record["body"])

def base64_image_to_text(bs64):
    img = Image.open(io.BytesIO(base64.b64decode(bs64)))
    price = pytesseract.image_to_string(img)
    return ''.join(re.findall(r"\d+", price))


def to_price_datapoint(data):
    price = base64_image_to_text(data["price-data-url"].split(',')[1]) if data["price-data-url"] else None
    year = base64_image_to_text(data["year-data-url"].split(',')[1]) if data["year-data-url"] else None
    data["price"] = price
    data["year"] = year
    data.pop("price-data-url")
    data.pop("year-data-url")
    return data

def send_message(message):
    sqs.send_message(message)


def process_price(event, context):
    records = event["Records"]
    prices = map(sqs_message_to_dict, records)
    prices = map(to_price_datapoint, prices)
    list(map(send_message, prices))



def main():
    message =  "{\"address\":\"14 Finch Street\",\"price-data-url\":\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEgAAAAPCAYAAABHsImTAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAEnSURBVFhH7ZWLDcMgDESz/ybZgA3YgAlYIBO45ucCNlbqqpEq8SSkmjvT2EBybDabRwCI4M8TTjY8KgDVxgC4IDj0uYC/PvddwY3/1+kQvTg/o/msGoMa5ON7AWGuZ2iqVoDii17ObTkuJGX9HJrPqolIJu10kL+NRYM0H60vFV53F6Ws5UYKp1nzWbUUM+QGrTubtVxs9WgNWvhofRoO0oYmrVy9ddzQfFYtxQz+sHUIzemhvEWDGpKv30U6TXUX7xag+axaihlUQG1IOXLljmbDgm8aNGMpQPNZtRQz5gZRrCUhv2rQX7yDygOPczN3Ck9IPvGKVb35y1dGeZkrPqsmQgV0JkrEeZwWkynPeILaVS5j3EG8B+Capqyv+azaZrN5gON4AavURGI7WAUpAAAAAElFTkSuQmCC\",\"year-data-url\":\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAPCAYAAACFgM0XAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAADsSURBVEhL7ZPRDcMgDETZfxM2YAM2YAIWyASODxtMqJNG7Uf7kZMsJZyxH04Ij/5KW0kUY9RIVDYitQLVbF4qxNbwIOKVknwP6rVzffWaaCuUOCFxV7zXbMWIKmX1+nPkSm0ja6whXDjzTwFWCbFMoZ++b25wMXNZaGqOcAAkX+IWgBWVJjMM/AMccltT3bMACDznluMhTjW+JYrpmK8A8A4N6Alg1ELXZYqnsnHJ6bH2KYDk6ae6AyAb0HwpfvEPtATWCnCY5BIuRL8FXkIvLrfAxqp2kzeBWW8nYKefwyYxA3pNvgZ49BuFsAOTL1feX12gQQAAAABJRU5ErkJggg==\",\"site-id\":\"property24-sold-prices\",\"entity-type\":\"price\",\"url\":\"https://www.property24.com/property-values/finch-street/horison/roodepoort/gauteng/718\",\"date-scraped\":1665057002472}"
    event = {
        "Records": [{"body": {"Message": message}}]
    }
    process_price(event, {})


if __name__ == "__main__":
    main()
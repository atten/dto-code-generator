from flask import Flask, request


app = Flask(__name__)


def _item() -> dict:
    return {
        "timestamp": "2024-07-14T12:00:00Z",
        "enum_value": "value 1",
        "customName": 100.5,
        "list_value": [100, 200, 300]
    }


@app.get("/api/v1/basic")
def get_basic_dto_list():
    return [_item()]


@app.get("/api/v1/basic/<timestamp>")
def get_basic_dto_by_timestamp(timestamp: str):
    result = _item()
    result['timestamp'] = timestamp
    return result


@app.post("/api/v1/basic")
def create_basic_dto():
    return _item()


@app.post("/api/v1/basic/bulk")
def create_basic_dto_bulk():
    return [_item(), _item(), _item()]

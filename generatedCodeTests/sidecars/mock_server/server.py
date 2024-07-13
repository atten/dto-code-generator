from flask import Flask, request
import json


app = Flask(__name__)

@app.get("/api/v1/basic")
def get_basic_dto_list():
    return [
        {
            "timestamp": "2024-07-14T12:00:00Z",
            "enum_value": "value 1",
            "customName": 100.5,
            "list_value": [100, 200, 300]
        }
    ]

@app.post("/api/v1/basic")
def create_basic_dto():
    return json.loads(request.get_data())

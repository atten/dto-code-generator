from flask import Flask

from utils import item_factory, auth_required

app = Flask(__name__)


@app.get("/api/v1/ping")
@auth_required
def ping():
    return 'pong'


@app.get("/api/v1/basic")
@auth_required
def get_basic_dto_list():
    return [item_factory()]


@app.get("/api/v1/basic/<timestamp>")
@auth_required
def get_basic_dto_by_timestamp(timestamp: str):
    result = item_factory()
    result['timestamp'] = timestamp
    return result


@app.post("/api/v1/basic")
@auth_required
def create_basic_dto():
    return item_factory()


@app.post("/api/v1/basic/bulk")
@auth_required
def create_basic_dto_bulk():
    return [item_factory(), item_factory(), item_factory()]

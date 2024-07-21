import os
from functools import wraps

from flask import request, abort

ALLOWED_USER_AGENT = os.environ.get('ALLOWED_USER_AGENT')
AUTH_HEADER_NAME = os.environ.get('AUTH_HEADER_NAME')
AUTH_HEADER_VALUE = os.environ.get('AUTH_HEADER_VALUE')


def auth_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if ALLOWED_USER_AGENT and request.headers.get('user-agent') != ALLOWED_USER_AGENT:
            return abort(403)
        if AUTH_HEADER_NAME and request.headers.get(AUTH_HEADER_NAME) != AUTH_HEADER_VALUE:
            return abort(403)
        return f(*args, **kwargs)
    return decorated_function


def item_factory() -> dict:
    return {
        "timestamp": "2024-07-14T12:00:00Z",
        "duration": "PT10H50S",
        "enum_value": "value 1",
        "customName": 100.5,
        "list_value": [100, 200, 300]
    }

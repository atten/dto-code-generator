import os
from generated.api import TestApiClient, BasicDTO, ENUM_VALUE_VALUE_1

import pytest
import types
from datetime import datetime, timezone, timedelta
from marshmallow.exceptions import ValidationError


BASE_URL = os.environ['BASE_URL']
SECURED_BASE_URL = os.environ['SECURED_BASE_URL']


def test_import():
    from generated.api import TestApiClient  # noqa


def test_get():
    api = TestApiClient(base_url=BASE_URL)
    timestamp = datetime.now(tz=timezone.utc)
    result = api.get_basic_dto_by_timestamp(timestamp)

    assert isinstance(result, BasicDTO)


def test_get_list():
    api = TestApiClient(base_url=BASE_URL)
    result = api.get_basic_dto_list()

    assert isinstance(result, types.GeneratorType)
    assert isinstance(next(result), BasicDTO)


def test_post():
    api = TestApiClient(base_url=BASE_URL)
    item = BasicDTO(
        timestamp=datetime.now(),
        duration=timedelta(minutes=5),
        enum_value=ENUM_VALUE_VALUE_1,
        documented_value=2.5,
        list_value=[50, 100, 150]
    )
    result = api.create_basic_dto(item)

    assert isinstance(result, BasicDTO)


def test_post_list_required_fields_only():
    api = TestApiClient(base_url=BASE_URL)
    item = BasicDTO(
        timestamp=datetime.now(),
        duration=timedelta(minutes=5),
        enum_value=ENUM_VALUE_VALUE_1,
        documented_value=2.5,
        list_value=[50, 100, 150]
    )
    payload = [item]
    result = api.create_basic_dto_bulk(payload)

    assert isinstance(result, types.GeneratorType)
    assert isinstance(next(result), BasicDTO)


def test_post_request_wrong_enum_value():
    api = TestApiClient('http://none')
    item = BasicDTO(
        timestamp=datetime.now(),
        duration=timedelta(minutes=5),
        enum_value=ENUM_VALUE_VALUE_1 + 'azaza',
        documented_value=2.5,
        list_value=[50, 100, 150]
    )
    payload = [item]

    with pytest.raises(ValidationError):
        result = api.create_basic_dto_bulk(payload)
        next(result)


def test_403():
    api = TestApiClient(base_url=SECURED_BASE_URL, max_retries=1)
    with pytest.raises(RuntimeError):
        api.ping()


def test_user_agent_and_headers():
    api = TestApiClient(
        base_url=SECURED_BASE_URL,
        user_agent=os.environ['SECURED_USER_AGENT'],
        headers={
            os.environ['SECURED_HEADER_NAME']: os.environ['SECURED_HEADER_VALUE']
        }
    )
    api.ping()

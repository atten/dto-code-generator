import os

import pytest
import types
from datetime import datetime, timezone
from marshmallow.exceptions import ValidationError


def get_base_url() -> str:
    return os.environ.get('BASE_URL')


def test_import():
    from generated.api import TestApiClient  # noqa


def test_get():
    from generated.api import TestApiClient, BasicDTO

    api = TestApiClient(base_url=get_base_url())
    timestamp = datetime.now(tz=timezone.utc)
    result = api.get_basic_dto_by_timestamp(timestamp)

    assert isinstance(result, BasicDTO)


def test_get_list():
    from generated.api import TestApiClient, BasicDTO

    api = TestApiClient(base_url=get_base_url())
    result = api.get_basic_dto_list()

    assert isinstance(result, types.GeneratorType)
    assert isinstance(next(result), BasicDTO)


def test_post():
    from generated.api import TestApiClient, BasicDTO, ENUM_VALUE_VALUE_1

    api = TestApiClient(base_url=get_base_url())
    item = BasicDTO(
        timestamp=datetime.now(),
        enum_value=ENUM_VALUE_VALUE_1,
        documented_value=2.5,
        list_value=[50, 100, 150]
    )
    result = api.create_basic_dto(item)

    assert isinstance(result, BasicDTO)


def test_post_list_required_fields_only():
    from generated.api import TestApiClient, BasicDTO, ENUM_VALUE_VALUE_1

    api = TestApiClient(base_url=get_base_url())
    item = BasicDTO(
        timestamp=datetime.now(),
        enum_value=ENUM_VALUE_VALUE_1,
        documented_value=2.5,
        list_value=[50, 100, 150]
    )
    payload = [item]
    result = api.create_basic_dto_bulk(payload)

    assert isinstance(result, types.GeneratorType)
    assert isinstance(next(result), BasicDTO)


def test_post_request_wrong_enum_value():
    from generated.api import TestApiClient, BasicDTO, ENUM_VALUE_VALUE_1

    api = TestApiClient('http://none')
    item = BasicDTO(
        timestamp=datetime.now(),
        enum_value=ENUM_VALUE_VALUE_1 + 'azaza',
        documented_value=2.5,
        list_value=[50, 100, 150]
    )
    payload = [item]

    with pytest.raises(ValidationError):
        result = api.create_basic_dto_bulk(payload)
        next(result)

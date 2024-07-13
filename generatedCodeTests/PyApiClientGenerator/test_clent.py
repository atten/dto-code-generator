import os

import pytest
import types
from datetime import datetime
from marshmallow.exceptions import ValidationError


def get_base_url() -> str:
    return os.environ.get('BASE_URL')   #


def test_import():
    from generated.api import TestApiClient  # noqa


def test_get_request():
    from generated.api import TestApiClient, BasicDTO

    api = TestApiClient(base_url=get_base_url())
    result = api.get_basic_dto_list()
    assert isinstance(result, types.GeneratorType)
    assert isinstance(next(result), BasicDTO)


def test_post_request_required_only():
    from generated.api import TestApiClient, BasicDTO, ENUM_VALUE_VALUE_1

    api = TestApiClient(base_url=get_base_url())
    payload = BasicDTO(
        timestamp=datetime.now(),
        enum_value=ENUM_VALUE_VALUE_1,
        documented_value=2.5,
        list_value=[50, 100, 150]
    )

    result = api.create_basic_dto_list(payload)
    assert isinstance(result, BasicDTO)


def test_post_request_wrong_enum_value():
    from generated.api import TestApiClient, BasicDTO, ENUM_VALUE_VALUE_1

    api = TestApiClient('http://none')
    payload = BasicDTO(
        timestamp=datetime.now(),
        enum_value=ENUM_VALUE_VALUE_1 + 'azaza',
        documented_value=2.5,
        list_value=[50, 100, 150]
    )

    with pytest.raises(ValidationError):
        api.create_basic_dto_list(payload)

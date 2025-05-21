import os
from generated.api import Generated, AllDataclassesCollection as dto, AllConstantsCollection as constants

import pytest
import types
from datetime import datetime, timezone, timedelta
from marshmallow.exceptions import ValidationError


BASE_URL = os.environ['BASE_URL']
SECURED_BASE_URL = os.environ['SECURED_BASE_URL']


@pytest.mark.asyncio
async def test_get():
    api = Generated(base_url=BASE_URL)
    timestamp = datetime.now(tz=timezone.utc)
    result = await api.get_basic_dto_by_timestamp(timestamp)

    assert isinstance(result, dto.BasicDto)


@pytest.mark.asyncio
async def test_get_list():
    api = Generated(base_url=BASE_URL)
    result = api.get_basic_dto_list()

    assert isinstance(result, types.AsyncGeneratorType)
    async for item in result:
        assert isinstance(item, dto.BasicDto)


@pytest.mark.asyncio
async def test_post():
    api = Generated(base_url=BASE_URL)
    item = dto.BasicDto(
        timestamp=datetime.now(),
        duration=timedelta(minutes=5),
        enum_value=constants.ENUM_VALUE_VALUE_1,
        documented_value=2.5,
        list_value=[50, 100, 150]
    )
    result = await api.create_basic_dto(item)

    assert isinstance(result, dto.BasicDto)


@pytest.mark.asyncio
async def test_post_list_required_fields_only():
    api = Generated(base_url=BASE_URL)
    item = dto.BasicDto(
        timestamp=datetime.now(),
        duration=timedelta(minutes=5),
        enum_value=constants.ENUM_VALUE_VALUE_1,
        documented_value=2.5,
        list_value=[50, 100, 150]
    )
    payload = [item]
    result = api.create_basic_dto_bulk(payload)

    assert isinstance(result, types.AsyncGeneratorType)
    async for item in result:
        assert isinstance(item, dto.BasicDto)


@pytest.mark.asyncio
async def test_post_empty_list():
    api = Generated(base_url=BASE_URL)
    payload = []
    result = api.create_basic_dto_bulk(payload)

    assert isinstance(result, types.AsyncGeneratorType)
    async for _ in result:
        assert 1 is 2   # should not happen


@pytest.mark.asyncio
async def test_post_request_wrong_enum_value():
    api = Generated()
    item = dto.BasicDto(
        timestamp=datetime.now(),
        duration=timedelta(minutes=5),
        enum_value=constants.ENUM_VALUE_VALUE_1 + 'azaza',
        documented_value=2.5,
        list_value=[50, 100, 150]
    )
    payload = [item]
    result = api.create_basic_dto_bulk(payload)

    with pytest.raises(ValidationError):
        async for _ in result:
            pass


@pytest.mark.asyncio
async def test_403():
    api = Generated(base_url=SECURED_BASE_URL, max_retries=0)
    with pytest.raises(RuntimeError):
        await api.ping()


@pytest.mark.asyncio
async def test_user_agent_and_headers():
    api = Generated(
        base_url=SECURED_BASE_URL,
        user_agent=os.environ['SECURED_USER_AGENT'],
        headers={
            os.environ['SECURED_HEADER_NAME']: os.environ['SECURED_HEADER_VALUE']
        }
    )
    await api.ping()


@pytest.mark.asyncio
async def test_use_debug_curl():
    api = Generated(base_url="http://none", use_debug_curl=True, max_retries=0)
    with pytest.raises(RuntimeError) as e:
        await api.ping()

    assert 'curl "http://none/api/v1/ping"' in str(e)


@pytest.mark.asyncio
async def test_custom_exception_class():
    class ApiError(Exception):
        pass

    api = Generated(base_url="http://none", max_retries=0, exception_class=ApiError)
    with pytest.raises(ApiError):
        await api.ping()

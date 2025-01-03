# Auto-generated by DTO-Codegen TEST_VERSION, do not edit

from dataclasses import dataclass
from dataclasses import field
from dataclasses import is_dataclass
from datetime import datetime
from datetime import timedelta
from datetime import timezone
from decimal import Decimal
from typeguard import typechecked
from urllib.parse import urljoin, urlencode
import aiohttp
import asyncio
import logging
import marshmallow
import marshmallow_dataclass
import os
import re
import typing as t


class Generated:
    @typechecked
    def __init__(
        self,
        base_url: str = '',
        headers: t.Optional[t.Dict[str, str]] = None,
        logger: t.Union[logging.Logger, t.Callable[[str], None], None] = None,
        max_retries: int = int(os.environ.get('API_CLIENT_MAX_RETRIES', 5)),
        retry_timeout: float = float(os.environ.get('API_CLIENT_RETRY_TIMEOUT', 3)),
        user_agent: t.Optional[str] = os.environ.get('API_CLIENT_USER_AGENT'),
        use_response_streaming = bool(int(os.environ.get('API_CLIENT_USE_STREAMING', 1))),
        use_request_payload_validation: bool = bool(int(os.environ.get('API_CLIENT_USE_REQUEST_PAYLOAD_VALIDATION', 1))),
        use_debug_curl: bool = bool(int(os.environ.get('API_CLIENT_USE_DEBUG_CURL', 0))),
        request_kwargs: t.Optional[t.Dict[str, t.Any]] = None,
        connection_pool_kwargs: t.Optional[t.Dict[str, t.Any]] = None,
    ):
        """
        API client constructor and configuration method.

        :param base_url: protocol://url[:port]
        :param headers: dict of HTTP headers (e.g. tokens)
        :param logger: logger instance (or callable like print()) for requests diagnostics
        :param max_retries: number of connection attempts before RuntimeException raise
        :param retry_timeout: seconds between attempts
        :param user_agent: request header
        :param use_response_streaming: enable alternative JSON library for deserialization (lower latency and memory footprint)
        :param use_request_payload_validation: enable client-side validation of serialized data before send
        :param use_debug_curl: include curl-formatted data for requests diagnostics
        :param request_kwargs: optional request arguments
        :param connection_pool_kwargs: optional arguments for internal connection pool
        """
        self._client = BaseJsonHttpAsyncClient(
            base_url=base_url,
            logger=logger,
            max_retries=max_retries,
            retry_timeout=retry_timeout,
            user_agent=user_agent,
            headers=headers,
            use_response_streaming=use_response_streaming,
            use_debug_curl=use_debug_curl,
            request_kwargs=request_kwargs or {},
            connection_pool_kwargs=connection_pool_kwargs or {},
        )

        self._deserializer = BaseDeserializer(
            use_response_streaming=use_response_streaming
        )

        self._serializer = BaseSerializer(
            self._deserializer,
            use_request_payload_validation=use_request_payload_validation
        )


    async def some_action(self, enum: str):
        await self._client.fetch(
            url=f'api/v1/action/{enum}',
            method='POST',
        )

    async def get_basic_dto_list(self) -> t.AsyncIterator['BasicDto']:
        """
        endpoint description
        """
        raw_data = await self._client.fetch(
            url='api/v1/basic',
        )
        for item in self._deserializer.deserialize(raw_data, BasicDto, many=True):
            yield item

    async def create_basic_dto(self, item: 'BasicDto') -> 'BasicDto':
        item = self._serializer.serialize(item, is_payload=True)
        raw_data = await self._client.fetch(
            url='api/v1/basic',
            method='POST',
            json_body=item,
        )
        gen = self._deserializer.deserialize(raw_data, BasicDto)
        return next(gen)

    async def create_basic_dto_bulk(self, items: t.Sequence['BasicDto']) -> t.AsyncIterator['BasicDto']:
        items = self._serializer.serialize(items, is_payload=True)
        raw_data = await self._client.fetch(
            url='api/v1/basic/bulk',
            method='POST',
            json_body=items,
        )
        for item in self._deserializer.deserialize(raw_data, BasicDto, many=True):
            yield item

    async def get_basic_dto_by_timestamp(self, timestamp: datetime) -> 'BasicDto':
        timestamp = self._serializer.serialize(timestamp)
        raw_data = await self._client.fetch(
            url=f'api/v1/basic/{timestamp}',
        )
        gen = self._deserializer.deserialize(raw_data, BasicDto)
        return next(gen)

    async def ping(self):
        await self._client.fetch(
            url='api/v1/ping',
        )


class BaseSchema(marshmallow.Schema):
    class Meta:
        # allow backward-compatible changes when new fields have added (simply ignore them)
        unknown = marshmallow.EXCLUDE


class JavaDurationField(marshmallow.fields.Field):

    def _deserialize(self, value, attr, data, **kwargs):
        try:
            return str_java_duration_to_timedelta(value)
        except ValueError as error:
            raise marshmallow.ValidationError(str(error)) from error

    def _serialize(self, value: t.Optional[timedelta], attr: str, obj, **kwargs):
        if value is None:
            return None
        return timedelta_to_java_duration(value) if value else "PT0S"


def str_java_duration_to_timedelta(duration: str) -> timedelta:
    """
    :param duration: string duration:'PT5S', 'PT10H59S' etc
    :return: timedelta()
    """
    groups = re.findall(r'PT(\d+H)?(\d+M)?([\d.]+S)?', duration)[0]
    if not groups:
        raise ValueError('Invalid duration: %s' % duration)

    hours, minutes, seconds = groups

    hours = int((hours or '0H').rstrip('H'))
    minutes = int((minutes or '0M').rstrip('M'))
    seconds = float((seconds or '0S').rstrip('S'))

    return timedelta(hours=hours, minutes=minutes, seconds=seconds)


def timedelta_to_java_duration(delta: timedelta) -> str:
    """
    Converts a timedelta to java duration string format
    Milliseconds are discarded

    >>> timedelta_to_java_duration(timedelta(minutes=15))
    'PT900S'

    >>> timedelta_to_java_duration(timedelta(days=1, minutes=21, seconds=35))
    'PT87695S'

    >>> timedelta_to_java_duration(timedelta(microseconds=123456))
    'PT0S'
    """
    seconds = delta.total_seconds()
    return 'PT{}S'.format(int(seconds))


ENUM_VALUE_VALUE_1 = "value 1"
ENUM_VALUE_VALUE_2 = "value 2"
ENUM_VALUE_VALUE_3 = "value 3"
ENUM_VALUES = [ENUM_VALUE_VALUE_1, ENUM_VALUE_VALUE_2, ENUM_VALUE_VALUE_3]


@dataclass
class BasicDto:
    timestamp: datetime = field(metadata=dict(marshmallow_field=marshmallow.fields.DateTime()))
    duration: timedelta = field(metadata=dict(marshmallow_field=JavaDurationField()))
    enum_value: str = field(metadata=dict(marshmallow_field=marshmallow.fields.String(validate=[marshmallow.fields.validate.OneOf(ENUM_VALUES)])))
    # short description
    # very long description lol
    documented_value: float = field(metadata=dict(marshmallow_field=marshmallow.fields.Float(data_key="customName")))
    list_value: list[int] = field(metadata=dict(marshmallow_field=marshmallow.fields.List(marshmallow.fields.Integer())))
    optional_value: float = field(metadata=dict(marshmallow_field=marshmallow.fields.Float()), default=0)
    nullable_value: t.Optional[bool] = field(metadata=dict(marshmallow_field=marshmallow.fields.Boolean(allow_none=True)), default=None)
    optional_list_value: list[int] = field(metadata=dict(marshmallow_field=marshmallow.fields.List(marshmallow.fields.Integer())), default_factory=list)


JSON_PAYLOAD = t.Union[dict, str, int, float, list]
RESPONSE_BODY = JSON_PAYLOAD


class BaseJsonHttpAsyncClient:
    def __init__(
        self,
        base_url: str,
        logger: t.Union[logging.Logger, t.Callable[[str], None], None],
        max_retries: int,
        retry_timeout: float,
        user_agent: t.Optional[str],
        headers: t.Optional[t.Dict[str, str]],
        use_response_streaming: bool,
        use_debug_curl: bool,
        request_kwargs: dict,
        connection_pool_kwargs: dict,
    ):
        self._base_url = base_url
        self._logger = logger
        self._max_retries = max_retries
        self._retry_timeout = retry_timeout
        self._user_agent = user_agent
        self._headers = headers
        self._use_debug_curl = use_debug_curl
        self._request_kwargs = request_kwargs

    async def fetch(
        self,
        url: str,
        method: str = 'get',
        query_params: t.Optional[dict] = None,
        json_body: t.Optional[JSON_PAYLOAD] = None,
        form_fields: t.Optional[t.Dict[str, str]] = None,
    ) -> RESPONSE_BODY:
        """
        Retrieve JSON response from remote API request.

        Repeats request in case of network errors.

        :param url: target url (relative to base url)
        :param method: HTTP verb, e.g. get/post
        :param query_params: key-value arguments like ?param1=11&param2=22
        :param json_body: JSON-encoded HTTP body
        :param form_fields: form-encoded HTTP body
        :return: decoded JSON from server
        """
        full_url = self._get_full_url(url, query_params)
        headers = self._headers.copy() if self._headers else dict()
        if json_body is not None:
            headers['content-type'] = 'application/json'
        if form_fields is not None:
            json_body = urlencode(form_fields)
            headers['content-type'] = 'application/x-www-form-urlencoded'
        if self._user_agent:
            headers['user-agent'] = self._user_agent

        request_kwargs = self._request_kwargs.copy()
        request_kwargs.update(
            full_url=full_url,
            method=method,
            headers=headers,
            body=json_body,
        )

        try:
            return await failsafe_call_async(
                self._mk_request,
                kwargs=request_kwargs,
                exceptions=(aiohttp.ClientConnectorError, ConnectionRefusedError),
                logger=self._logger,
                max_attempts=self._max_retries,
                on_transitional_fail=lambda exc, info: asyncio.sleep(self._retry_timeout)
            )
        except Exception as e:
            if self._use_debug_curl:
                curl_cmd = build_curl_command(
                    url=full_url,
                    method=method,
                    headers=headers,
                    body=json_body,
                )
                raise RuntimeError(f'Failed to {curl_cmd}: {e}') from e
            raise RuntimeError(f'Failed to {method} {full_url}: {e}') from e

    @classmethod
    async def _mk_request(cls, full_url: str, method: str, body: t.Optional[JSON_PAYLOAD], headers: t.Optional[dict]) -> RESPONSE_BODY:
        async with aiohttp.request(
            url=full_url,
            method=method,
            headers=headers,
            json=body,
        ) as response:
            response.raise_for_status()
            return await response.json()

    def _get_full_url(self, url: str, query_params: t.Optional[dict] = None) -> str:
        if self._base_url:
            url = urljoin(self._base_url, url)

        if query_params:
            query_tuples = []
            for key, value in query_params.items():
                if isinstance(value, (list, tuple)):
                    for item in value:
                        query_tuples.append((key, item))
                else:
                    query_tuples.append((key, value))

            if '?' in url:
                url += '&' + urlencode(query_tuples)
            else:
                url += '?' + urlencode(query_tuples)

        return url


class BaseSerializer:
    def __init__(self, deserializer: 'BaseDeserializer', use_request_payload_validation: bool):
        self._use_request_payload_validation = use_request_payload_validation
        self._deserializer = deserializer

    def serialize(self, value: t.Any, is_payload=False) -> t.Optional[JSON_PAYLOAD]:
        # auto-detect collections
        many = False
        _type = type(value)
        if isinstance(value, (list, tuple, set)) and value:
            # non-empty sequence
            _type = type(next(iter(value)))
            many = True

        # pick built-in serializer if specified for class
        method_name = '_serialize_{type}'.format(type=_type.__name__.lower())
        if hasattr(self, method_name):
            method = getattr(self, method_name)
            if many:
                return list(map(method, value))
            return method(value)

        # use marshmallow in case of dataclass
        if is_dataclass(_type):
            schema = marshmallow_dataclass.class_schema(_type)()
            func = schema.dump if is_payload else schema.dumps
            serialized_data = func(value, many=many)

            if self._use_request_payload_validation:
                gen = self._deserializer.deserialize(serialized_data, _type, many=many)
                for _ in gen:
                    pass

            return serialized_data

        if isinstance(value, t.get_args(JSON_PAYLOAD)):
            return value

        if value is None:
            if not is_payload:
                # special case for null values in URL
                return ''
            return None

        raise ValueError('Unable to serialize object of type {0}: {1}'.format(type(value), value))

    @classmethod
    def _serialize_datetime(cls, value: datetime) -> str:
        # use ISO format: YYYY-MM-DDTHH:mm:ss[.ms]Z
        value = value.astimezone(timezone.utc).isoformat()
        if value.endswith('+00:00'):
            value = value[:-6] + 'Z'
        return value

    @classmethod
    def _serialize_timestamp(cls, value) -> str:
        # for pandas.Timestamp
        return cls._serialize_datetime(value.to_pydatetime())

    @classmethod
    def _serialize_timedelta(cls, value: timedelta) -> str:
        return '{seconds}s'.format(seconds=int(value.total_seconds()))

    @classmethod
    def _serialize_decimal(cls, value: Decimal) -> str:
        return str(value)


class BaseDeserializer:
    def __init__(self, use_response_streaming: bool):
        self._use_response_streaming = use_response_streaming

    def deserialize(self, raw_data: RESPONSE_BODY, data_class: t.Optional[t.Type] = None, many: bool = False) -> t.Iterator[t.Any]:
        if hasattr(raw_data, 'read'):
            # read singular JSON objects at once and multiple objects in stream to reduce memory footprint
            if many and self._use_response_streaming:
                raw_data = ijson.items(raw_data, 'item', use_float=True)
            else:
                raw_data = json.loads(raw_data.read())

        if raw_data == '':
            # blank response means null
            yield None
            return

        if data_class is None:
            # skip further deserialization
            if many:
                yield from raw_data
            else:
                yield raw_data
            return

        # pick built-in deserializer if specified for class
        method_name = '_deserialize_{type}'.format(type=data_class.__name__.lower())
        if hasattr(self, method_name):
            method = getattr(self, method_name)
            if many:
                yield from map(method, raw_data)
            else:
                yield method(raw_data)
        else:
            try:
                # use marshmallow in other cases
                schema = marshmallow_dataclass.class_schema(data_class, base_schema=BaseSchema)()
            except TypeError:
                # fallback to default constructor
                yield data_class(raw_data)
                return

            if many:
                yield from map(schema.load, raw_data)
            else:
                yield schema.load(raw_data)

    @classmethod
    def _deserialize_datetime(cls, raw: str) -> datetime:
        if raw.endswith('Z'):
            raw = raw[:-1] + '+00:00'
        return datetime.fromisoformat(raw)

    @classmethod
    def _deserialize_timedelta(cls, raw: str) -> timedelta:
        if raw.endswith('s'):
            return timedelta(seconds=int(raw[:-1]))
        raise NotImplementedError(f'Unsupported value for timedelta deserialization: {raw}')


def _get_func_name_verbose(func: t.Callable) -> str:
    if hasattr(func, '__self__'):
        if hasattr(func.__self__, '__name__'):
            return '{}.{}'.format(func.__self__.__name__, func.__name__)
        return '{}.{}'.format(func.__self__.__class__.__name__, func.__name__)
    elif hasattr(func, '__name__'):
        return func.__name__
    return repr(func)


async def failsafe_call_async(
    func: t.Callable,
    exceptions: t.Iterable[t.Type[Exception]],
    args=None,
    kwargs=None,
    logger: t.Union[logging.Logger, t.Callable[[str], None]] = None,
    attempt=1,
    max_attempts=10,
    on_transitional_fail: t.Callable[[Exception, dict], t.Any] = None,
):
    args = args or tuple()
    kwargs = kwargs or dict()
    func_name_verbose = _get_func_name_verbose(func)

    try:
        return await func(*args, **kwargs)
    except exceptions as e:
        if logger:
            message = 'got %s on %s, attempt %d / %d' % (
                e.__class__.__name__,
                func_name_verbose,
                attempt,
                max_attempts
            )
            if hasattr(logger, 'warning'):
                logger.warning(message)
            else:
                logger(message)

        if attempt >= max_attempts:
            raise e from None   # suppress context and multiple tracebacks of same error

        if on_transitional_fail:
            await on_transitional_fail(e, dict(max_attempts=max_attempts, attempt=attempt))

        return await failsafe_call_async(
            func,
            exceptions,
            args,
            kwargs,
            logger,
            attempt + 1,
            max_attempts,
            on_transitional_fail
        )


def build_curl_command(url: str, method: str, headers: t.Dict[str, str], body: str) -> str:
    """
    >>> build_curl_command('https://example.com', 'get', {}, '')
    'curl "https://example.com"'

    >>> build_curl_command('https://example.com?param1=value1&param2=value2', 'post', {'content-type': 'application/json'}, '{"foo": "bar"}')
    'curl "https://example.com?param1=value1&param2=value2" -X POST -H "content-type: application/json" -d "{\"foo\": \"bar\"}"'
    """
    method = method.upper()

    if method != 'GET':
        method = f' -X {method}'
    else:
        method = ''

    headers = ''.join(f' -H "{k}: {v}"' for k, v in headers.items())

    if body:
        body = body.replace('"', '\"')
        body = f' -d "{body}"'
    else:
        body = ''

    return f'curl "{url}"{method}{headers}{body}'


__all__ = [
    "BasicDto",
    "ENUM_VALUES",
    "ENUM_VALUE_VALUE_1",
    "ENUM_VALUE_VALUE_2",
    "ENUM_VALUE_VALUE_3",
    "Generated",
]

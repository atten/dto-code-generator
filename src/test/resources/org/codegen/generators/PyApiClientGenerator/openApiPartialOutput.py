# Auto-generated by DTO-Codegen TEST_VERSION, do not edit

from dataclasses import dataclass
from dataclasses import field
from dataclasses import is_dataclass
from datetime import datetime
from datetime import timedelta
from datetime import timezone
from decimal import Decimal
from time import sleep
from typeguard import typechecked
from urllib.parse import urljoin, urlencode
import ijson
import io
import json
import logging
import marshmallow
import marshmallow_dataclass
import os
import typing as t
import urllib3


class BaseSchema(marshmallow.Schema):
    class Meta:
        # allow backward-compatible changes when new fields have added (simply ignore them)
        unknown = marshmallow.EXCLUDE


JSON_PAYLOAD = t.Union[dict, str, int, float, list]
RESPONSE_BODY = [str, io.IOBase]


class BaseJsonApiClient:
    base_url = ''
    default_max_retries = int(os.environ.get('API_CLIENT_MAX_RETRIES', 5))
    default_retry_timeout = float(os.environ.get('API_CLIENT_RETRY_TIMEOUT', 3))
    default_user_agent = os.environ.get('API_CLIENT_USER_AGENT')
    use_response_streaming = bool(int(os.environ.get('API_CLIENT_USE_STREAMING', 1)))
    use_request_payload_validation = bool(int(os.environ.get('API_CLIENT_USE_REQUEST_PAYLOAD_VALIDATION', 1)))

    @typechecked
    def __init__(
        self,
        base_url: str = '',
        logger: t.Union[logging.Logger, t.Callable[[str], None], None] = None,
        max_retries: int = default_max_retries,
        retry_timeout: float = default_retry_timeout,
        user_agent: t.Optional[str] = default_user_agent,
        headers: t.Optional[t.Dict[str, str]] = None,
    ):
        """
        Remote API client constructor.

        :param base_url: protocol://url[:port]
        :param logger: logger instance (or callable like print()) for requests diagnostics
        :param max_retries: number of connection attempts before RuntimeException raise
        :param retry_timeout: seconds between attempts
        :param user_agent: request header
        :param headers: dict of HTTP headers (e.g. tokens)
        """
        if base_url:
            self.base_url = base_url
        self.logger = logger
        self.pool = urllib3.PoolManager(retries=False)
        self.max_retries = max_retries
        self.retry_timeout = retry_timeout
        self.user_agent = user_agent
        self.headers = headers

    def get_base_url(self) -> str:
        return self.base_url

    def _fetch(
        self,
        url: str,
        method: str = 'get',
        query_params: t.Optional[dict] = None,
        payload: t.Optional[JSON_PAYLOAD] = None,
    ) -> RESPONSE_BODY:
        """
        Retrieve JSON response from remote API request.

        Repeats request in case of network errors.

        :param url: target url (relative to base url)
        :param method: HTTP verb, e.g. get/post
        :param query_params: key-value arguments like ?param1=11&param2=22
        :param payload: JSON-like HTTP body
        :return: decoded JSON from server
        """
        full_url = self._get_full_url(url, query_params)
        headers = self.headers.copy() if self.headers else dict()
        if payload:
            payload = json.dumps(payload).encode('utf8')
            headers['content-type'] = 'application/json'
        if self.user_agent:
            headers['user-agent'] = self.user_agent

        try:
            return failsafe_call(
                self._mk_request,
                kwargs=dict(
                    url=full_url,
                    method=method,
                    headers=headers,
                    body=payload,
                ),
                exceptions=(urllib3.exceptions.HTTPError,),  # include connection errors, HTTP >= 400
                logger=self.logger,
                max_attempts=self.max_retries,
                on_transitional_fail=lambda exc, info: sleep(self.retry_timeout)
            )
        except Exception as e:
            error_verbose = str(e)
            if ' at 0x' in error_verbose:
                # reduce noise in error description, e.g. in case of NewConnectionError
                error_verbose = error_verbose.split(':', maxsplit=1)[-1].strip()
            raise RuntimeError(f'Failed to {method} {full_url}: {error_verbose}') from e

    def _mk_request(self, *args, **kwargs) -> RESPONSE_BODY:
        response = self.pool.request(*args, **kwargs, preload_content=False)
        if response.status >= 400:
            raise urllib3.exceptions.HTTPError('Server respond with status code {status}: {data}'.format(
                status=response.status,
                data=response.data,
            ))

        if 'json' in response.headers.get('content-type', ''):
            # provide Bytes I/O for file-like JSON read
            return response

        # decode whole non-json response into string
        return response.data.decode()

    def _get_full_url(self, url: str, query_params: t.Optional[dict] = None) -> str:
        if self.get_base_url():
            url = urljoin(self.get_base_url(), url)

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

    def _serialize(self, value: t.Any, is_payload=False) -> t.Optional[JSON_PAYLOAD]:
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

            if self.use_request_payload_validation:
                gen = self._deserialize(serialized_data, _type, many=many)
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

    def _deserialize(self, raw_data: RESPONSE_BODY, data_class: t.Optional[t.Type] = None, many: bool = False) -> t.Iterator[t.Any]:
        if hasattr(raw_data, 'read'):
            # read singular JSON objects at once and multiple objects in stream to reduce memory footprint
            if many and self.use_response_streaming:
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


def failsafe_call(
    func: t.Callable,
    exceptions: t.Iterable[t.Type[Exception]],
    args=None,
    kwargs=None,
    logger: t.Union[logging.Logger, t.Callable[[str], None]] = None,
    attempt=1,
    max_attempts=10,
    on_transitional_fail: t.Callable[[Exception, dict], None] = None,
):
    args = args or tuple()
    kwargs = kwargs or dict()
    func_name_verbose = _get_func_name_verbose(func)

    try:
        return func(*args, **kwargs)
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
            on_transitional_fail(e, dict(max_attempts=max_attempts, attempt=attempt))

        return failsafe_call(
            func,
            exceptions,
            args,
            kwargs,
            logger,
            attempt + 1,
            max_attempts,
            on_transitional_fail
        )


SOME_ENUM_VARIANT1 = "variant1"
SOME_ENUM_VARIANT2 = "variant2"
SOME_ENUM_VARIANT3 = "variant3"
SOME_ENUMS = [SOME_ENUM_VARIANT1, SOME_ENUM_VARIANT2, SOME_ENUM_VARIANT3]


SOME_ENUM_ROCK = "ROCK"
SOME_ENUM_SCISSORS = "SCISSORS"
SOME_ENUM_PAPER = "PAPER"
ADVANCED_DTO_SOME_ENUMS = [SOME_ENUM_ROCK, SOME_ENUM_SCISSORS, SOME_ENUM_PAPER]


@dataclass
class AdvancedDTO:
    # Example: [{"foo": "bar"}]
    json: t.Optional[dict] = None
    # Enum field with the same name as of different entity
    some_enum: t.Optional[str] = field(metadata=dict(marshmallow_field=marshmallow.fields.String(allow_none=True, validate=[marshmallow.fields.validate.OneOf(ADVANCED_DTO_SOME_ENUMS)])), default=None)


@dataclass
class BasicDto:
    # Field description
    some_integer: int = field(metadata=dict(marshmallow_field=marshmallow.fields.Integer()))
    # Field description
    some_number: float = field(metadata=dict(marshmallow_field=marshmallow.fields.Float()))
    some_string: t.Optional[str] = field(metadata=dict(marshmallow_field=marshmallow.fields.String(allow_none=True)), default=None)
    some_boolean: t.Optional[bool] = field(metadata=dict(marshmallow_field=marshmallow.fields.Boolean(allow_none=True, data_key="someBoolean")), default=None)
    timestamp: t.Optional[datetime] = field(metadata=dict(marshmallow_field=marshmallow.fields.DateTime(allow_none=True)), default=None)
    some_enum: t.Optional[str] = field(metadata=dict(marshmallow_field=marshmallow.fields.String(allow_none=True, validate=[marshmallow.fields.validate.OneOf(SOME_ENUMS)])), default=None)
    nested_object: t.Optional[AdvancedDTO] = field(metadata=dict(marshmallow_field=marshmallow.fields.Nested(marshmallow_dataclass.class_schema(AdvancedDTO, base_schema=BaseSchema), allow_none=True)), default=None)


class TestApiClient(BaseJsonApiClient):
    def get_basic(self, page: t.Optional[int] = None, page_size: t.Optional[int] = None) -> t.Iterator[BasicDto]:
        """
        endpoint description
        """
        query_params = dict()
        if page is not None:
            query_params['page'] = page
        if page_size is not None:
            query_params['pageSize'] = page_size
        raw_data = self._fetch(
            url='/api/v1/basic',
            query_params=query_params,
        )
        yield from self._deserialize(raw_data, BasicDto, many=True)

    def post_basic(self, item: BasicDto) -> BasicDto:
        item = self._serialize(item, is_payload=True)
        raw_data = self._fetch(
            url='/api/v1/basic',
            method='POST',
            payload=item,
        )
        gen = self._deserialize(raw_data, BasicDto)
        return next(gen)


__all__ = [
    "ADVANCED_DTO_SOME_ENUMS",
    "AdvancedDTO",
    "BasicDto",
    "SOME_ENUMS",
    "SOME_ENUM_PAPER",
    "SOME_ENUM_ROCK",
    "SOME_ENUM_SCISSORS",
    "SOME_ENUM_VARIANT1",
    "SOME_ENUM_VARIANT2",
    "SOME_ENUM_VARIANT3",
    "TestApiClient",
]

# Auto-generated by DTO-Codegen TEST_VERSION, do not edit

from amqp.exceptions import RecoverableConnectionError, ConnectionForced
from dataclasses import asdict
from dataclasses import astuple
from dataclasses import dataclass
from dataclasses import field
from dataclasses import is_dataclass
from datetime import datetime
from datetime import timedelta
from datetime import timezone
from decimal import Decimal
from funcy import memoize
from gevent._semaphore import Semaphore
from gevent.event import AsyncResult
from kombu import Connection, Exchange, Queue, Message
from socket import timeout as SocketTimeout
from urllib.parse import urlparse
from uuid import uuid4
import gevent
import ijson
import io
import json
import logging
import marshmallow
import marshmallow_dataclass
import re
import time
import typing as t


class BaseSchema(marshmallow.Schema):
    class Meta:
        # allow backward-compatible changes when new fields have added (simply ignore them)
        unknown = marshmallow.EXCLUDE


RECOVERABLE_EXCEPTIONS = (ConnectionError, ConnectionResetError, IOError, ConnectionForced, RecoverableConnectionError)

JSON_PAYLOAD = t.Union[dict, str, int, float, list]
RESPONSE_BODY = [str, io.IOBase]


def _check_amqp_alive(connection: Connection, raise_exception=False) -> bool:
    try:
        connection.connect()
    except Exception as e:
        if raise_exception:
            raise ConnectionError('Failed to connect to %s: %s' % (connection.host, e)) from e
        return False
    return True


def _verbose_amqp_url(amqp_url: str) -> str:
    parsed = urlparse(amqp_url)
    return f'{parsed.hostname}:{parsed.port}'


def _clear_instance_memoized_data(memory_dict: dict, instance):
    """
    Clear memoized values of @funcy.memoize only for given instance.
    It is useful because build-in 'memory' dict stores memoized data for all instances.
    """
    keys_to_delete = []
    for key in memory_dict.keys():
        if key[0] is instance:
            keys_to_delete.append(key)

    for key in keys_to_delete:
        del memory_dict[key]


class AmqpWrapper:

    def __init__(self, amqp_url: str, read_exchange_name: str, read_queue_name: str, write_exchange_name: str = None,
                 prefetch_count: int = 30, logger: logging.Logger = None):
        self.amqp_url = amqp_url
        self.read_exchange_name = read_exchange_name
        self.read_queue_name = read_queue_name
        self.write_exchange_name = write_exchange_name
        self.prefetch_count = prefetch_count
        self.logger = logger or logging.getLogger(self.__class__.__name__)

        self.connection = None
        self.write_exchange = None
        self.is_connected = False  # self.connection could be outdated, refer to this flag
        self.is_listening = False
        self.is_stopped = False
        self.producer = None
        self._incoming_message_handlers = []

    def connect(self, force=False):
        """attempts to connect. If fails, will throw an exception"""
        if self.is_connected and not force:
            return

        self.is_connected = False
        self.is_listening = False
        # clear memoized values (only for current instance)
        _clear_instance_memoized_data(self.declare_read_queue.memory, self)
        self.connection = Connection(self.amqp_url)
        _check_amqp_alive(self.connection, raise_exception=True)

        if self.write_exchange_name:
            self.write_exchange = Exchange(self.write_exchange_name, 'direct', durable=True)
            self.producer = self.connection.Producer(serializer='json', on_return=self._handle_undelivered)

        # as soon as we tested connection and made the producer, switch the flag ON
        self.is_connected = True
        self.logger.info('{}: connected to {}'.format(self.__class__.__name__, _verbose_amqp_url(self.amqp_url)))

    def try_connect(self):
        try:
            self.connect()
        except RECOVERABLE_EXCEPTIONS:
            self._try_reconnect()

    def _try_reconnect(self):
        """makes 10 attempts to connect to the message broker every 5 sec, after than will throw an exception"""
        failsafe_call(
            self.connect,
            kwargs=dict(force=True),
            exceptions=RECOVERABLE_EXCEPTIONS,
            logger=self.logger,
            on_transitional_fail=lambda exc, info: time.sleep(5)
        )

    def listen(self, timeout=None, *args, **kwargs):
        """If timeout is set, listen each message no more than defined seconds. Listen forever otherwise."""
        self.is_listening = False
        self.try_connect()
        while True:
            try:
                read_queue = self.declare_read_queue(*args, **kwargs)
                with self.connection.Consumer(
                    read_queue,
                    callbacks=[self.handle],
                    prefetch_count=self.prefetch_count,
                    auto_declare=None,
                ):
                    self.is_connected = True
                    self.is_listening = True
                    timeout_verbose = f'(timeout={timeout}s)' if timeout else 'permanently'
                    self.logger.info(f'listen queue {read_queue.name} {timeout_verbose}')

                    # if connection.close() called, connection.connection set to None
                    while self.connection.connection is not None:
                        self.connection.drain_events(timeout=timeout)
            except SocketTimeout:
                if timeout:
                    return
                self._try_reconnect()
            except RECOVERABLE_EXCEPTIONS:
                self._try_reconnect()
            finally:
                self.is_listening = False

    def add_incoming_message_handler(self, func: t.Callable[[JSON_PAYLOAD, Message], None]):
        """Apply 'observer' pattern for listeners"""
        self._incoming_message_handlers.append(func)

    def handle(self, body: list, message: Message):
        if not self.is_connected:
            # prevents processing of downloaded messages after connection.close()
            # (for some reason another drain_events() cycle will be started)
            self.logger.warning(f'skip message {message} due to disconnected state')
            return

        # iterate over all listeners
        for func in self._incoming_message_handlers:
            func(body, message)

    def publish(self, data: t.Any, *args, **kwargs):
        self.try_connect()

        while True:
            try:
                self.logger.debug(f'send msg to {self.write_exchange} {kwargs}: {data}')
                return self.producer.publish(data, exchange=self.write_exchange, *args, **kwargs)
            except RECOVERABLE_EXCEPTIONS:
                self._try_reconnect()

    @memoize
    def declare_read_queue(self, *args, **kwargs) -> Queue:
        assert self.read_queue_name, 'read_queue_name must not be empty'  # prevent assigning random name by amqp
        self.connect()
        read_exchange = Exchange(self.read_exchange_name, 'direct', durable=True)
        read_queue = Queue(self.read_queue_name, exchange=read_exchange, *args, **kwargs)
        read_queue(self.connection).declare()
        return read_queue

    # noinspection PyUnusedLocal
    def _handle_undelivered(self, exception, exchange, routing_key: str, message):
        """
        When a message was not delivered using producer.publish(), that callback is being called
        """
        pass

    def stop(self):
        self.is_stopped = True
        self.is_connected = False
        if self.connection is not None:
            self.logger.info('{}: close connection {}'.format(self.__class__.__name__, _verbose_amqp_url(self.amqp_url)))
            self.connection.close()


@dataclass(frozen=True)
class AmqpRequest:
    id: str
    api_keys: dict
    response_routing_key: str
    func: str
    args: t.Sequence[JSON_PAYLOAD]


@dataclass(frozen=True)
class AmqpResponse:
    id: str
    result: JSON_PAYLOAD
    error: t.Optional[str]


class FailedAmqpRequestError(Exception):
    pass


class AsyncAmqpResult:
    """Uses gevent.AsyncResult with pre-defined timeout."""

    def __init__(self, request: AmqpRequest, timeout: int):
        self.timeout = timeout
        self.request = request
        self.event = AsyncResult()

    def set_exception(self, exc: Exception):
        """Put error for failure request."""
        self.event.set_exception(exc)

    def set(self, value: JSON_PAYLOAD):
        """Put result for successful request."""
        self.event.set(value)

    def get(self) -> JSON_PAYLOAD:
        """Retrieve result or throw exception."""
        try:
            return self.event.get(block=True, timeout=self.timeout)
        except gevent.Timeout as exc:
            request_dict = asdict(self.request)
            request_dict.pop('api_keys')
            description = 'Timeout exceeded while waiting for AMQP result (timeout={timeout}s, request={request})'.format(
                timeout=self.timeout,
                request=request_dict,
            )
            raise RuntimeError(description) from exc


class BaseAmqpApiClient(AmqpWrapper):
    """
    Wrapper for asynchronous interaction with AMQP instance through queues.
    """

    def __init__(self, api_keys: dict = None, high_priority: bool = False,
                 request_timeout: int = 3600, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.api_keys = api_keys
        self.high_priority = high_priority
        self.request_timeout = request_timeout

        self.pending_async_results = {}
        self.add_incoming_message_handler(self._process_async_result)

    def _process_async_result(self, body: JSON_PAYLOAD, message: Message):
        try:
            parsed_body = AmqpResponse(*body)
        except TypeError:
            # message does not fit signature, skip
            return

        result = self.pending_async_results.pop(parsed_body.id, None)
        message.ack()

        if result is None:
            # skip a response which was not requested
            # (most probably was requested by previous instance within same queue)
            self.logger.warning('skip message from %s: %s' % (self.read_queue_name, parsed_body.id))
        elif parsed_body.error:
            result.set_exception(FailedAmqpRequestError(parsed_body.error))
        else:
            self.logger.debug('process message from %s: %s' % (self.read_queue_name, parsed_body.id))
            result.set(parsed_body.result)

    @property
    def _read_queue_kwargs(self) -> dict:
        return dict(
            routing_key=self.read_queue_name,
            auto_delete=True,  # incoming queue is one-off (only for this client)
        )

    def _mk_request(self, routing_key: str, func: str, *args) -> AsyncAmqpResult:
        request = AmqpRequest(
            id=str(uuid4()),
            api_keys=self.api_keys or {},
            response_routing_key=self.read_queue_name,
            func=func,
            args=args
        )
        result = AsyncAmqpResult(request=request, timeout=self.request_timeout)

        kwargs = {
            'routing_key': routing_key,
        }
        if self.high_priority:
            kwargs['priority'] = 1

        self.pending_async_results[request.id] = result
        # declare incoming queue (if not yet done) before making of request, otherwise response may be lost
        self.declare_read_queue(**self._read_queue_kwargs)
        self.publish(astuple(request), **kwargs)
        return result


class AmqpApiWithLazyListener(BaseAmqpApiClient):
    """Composition of normal Gateway API + gevent background listener launched on demand"""

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._listener_greenlet = None
        self._lock = Semaphore()

    def ensure_listen(self):
        another_try = False

        with self._lock:
            if self._listener_greenlet and not self.is_listening:
                self._listener_greenlet.kill()

            if not self._listener_greenlet or self._listener_greenlet.dead:
                total_waiting = 0
                self.logger.debug('spawn %s' % self)
                self._listener_greenlet = gevent.spawn(self.listen, **self._read_queue_kwargs)

                while not self.is_listening:
                    gevent.sleep(0.1)
                    if self._listener_greenlet.exception:
                        raise self._listener_greenlet.exception

                    total_waiting += 0.1
                    if self.is_connected and total_waiting > 5:
                        # Sometimes AMQP client is freezing while making Consumer (with totally normal connection).
                        # This is utterly strange, IDK better workaround than restarting it from scratch.
                        self.logger.warning(
                            '{}: timeout exceeded for awaiting of listener, respawn'.format(self.__class__.__name__)
                        )
                        another_try = True
                        break

        # do recursion outside of lock
        if another_try:
            self.ensure_listen()

    def stop(self):
        if self._listener_greenlet:
            self._listener_greenlet.kill()
        super().stop()

    def _mk_request(self, routing_key: str, func: str, *args) -> AsyncAmqpResult:
        self.ensure_listen()
        return super()._mk_request(routing_key, func, *args)

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


ENUM_VALUE_VALUE_1 = "value 1"
ENUM_VALUE_VALUE_2 = "value 2"
ENUM_VALUE_VALUE_3 = "value 3"
ENUM_VALUES = [ENUM_VALUE_VALUE_1, ENUM_VALUE_VALUE_2, ENUM_VALUE_VALUE_3]


@dataclass
class BasicDTO:
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


class ApiClient(AmqpApiWithLazyListener):
    def some_action(self, enum: str):
        self._mk_request(f'api/v1/action/{enum}', 'some_action').get()

    def get_basic_dto_list(self) -> t.List[BasicDTO]:
        """
        endpoint description
        """
        raw_data = self._mk_request(f'api/v1/basic', 'get_basic_dto_list').get()
        return list(self._deserialize(raw_data, BasicDTO, many=True))

    def create_basic_dto(self, item: BasicDTO) -> BasicDTO:
        item = self._serialize(item, is_payload=True)
        args = (item,)
        raw_data = self._mk_request(f'api/v1/basic', 'create_basic_dto', *args).get()
        gen = self._deserialize(raw_data, BasicDTO)
        return next(gen)

    def create_basic_dto_bulk(self, items: t.Sequence[BasicDTO]) -> t.List[BasicDTO]:
        items = self._serialize(items, is_payload=True)
        args = (items,)
        raw_data = self._mk_request(f'api/v1/basic/bulk', 'create_basic_dto_bulk', *args).get()
        return list(self._deserialize(raw_data, BasicDTO, many=True))

    def get_basic_dto_by_timestamp(self, timestamp: datetime) -> BasicDTO:
        timestamp = self._serialize(timestamp)
        raw_data = self._mk_request(f'api/v1/basic/{timestamp}', 'get_basic_dto_by_timestamp').get()
        gen = self._deserialize(raw_data, BasicDTO)
        return next(gen)

    def ping(self):
        self._mk_request(f'api/v1/ping', 'ping').get()


__all__ = [
    "AmqpApiWithLazyListener",
    "ApiClient",
    "BasicDTO",
    "ENUM_VALUES",
    "ENUM_VALUE_VALUE_1",
    "ENUM_VALUE_VALUE_2",
    "ENUM_VALUE_VALUE_3",
    "FailedAmqpRequestError",
]

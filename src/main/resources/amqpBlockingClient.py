RECOVERABLE_EXCEPTIONS = (ConnectionError, ConnectionResetError, IOError, ConnectionForced, RecoverableConnectionError)

JSON_PAYLOAD = t.Union[dict, str, int, float, list]
RESPONSE_BODY = [str, io.IOBase]


class BaseSchema(marshmallow.Schema):
    class Meta:
        # allow backward-compatible changes when new fields have added (simply ignore them)
        unknown = marshmallow.EXCLUDE


def failsafe_call(
    func: t.Callable,
    exceptions: t.Iterable[t.Type[Exception]],
    args=None,
    kwargs=None,
    logger=None,
    attempt=1,
    max_attempts=10,
    on_transitional_fail: t.Callable = None
):
    args = args or tuple()
    kwargs = kwargs or dict()

    if hasattr(func, '__self__'):
        if hasattr(func.__self__, '__name__'):
            func_name_verbose = '{}.{}'.format(func.__self__.__name__, func.__name__)
        else:
            func_name_verbose = '{}.{}'.format(func.__self__.__class__.__name__, func.__name__)
    else:
        func_name_verbose = func.__name__

    try:
        return func(*args, **kwargs)
    except exceptions as e:
        if logger:
            logger.warning('got %s on %s, attempt %d / %d' % (
                e.__class__.__name__,
                func_name_verbose,
                attempt,
                max_attempts
            ))

        if attempt >= max_attempts:
            raise e

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


def _check_amqp_alive(connection: Connection, raise_exception=False) -> bool:
    try:
        connection.connect()
    except Exception as e:
        if raise_exception:
            raise ConnectionError('Failed to connect to %s: %s' % (connection.host, e))
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
                    timeout_verbose = f'(timeout={timeout}s)' if timeout else 'permanently'
                    self.is_listening = True
                    self.logger.info(f'listen queue {read_queue.name} {timeout_verbose}')

                    while True:
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
            # (for some reason will be started another drain_events() cycle)
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


class SyncAmqpResult:
    """Simple, single-threaded implementation without gevent."""
    def __init__(self, timeout: int):
        self._val = None
        self._exc = None

    def set_exception(self, exc: Exception):
        """Put error for failure request."""
        self._exc = exc

    def set(self, value: JSON_PAYLOAD):
        """Put result for successful request."""
        self._val = value

    def get(self) -> JSON_PAYLOAD:
        """Retrieve result or throw exception."""
        if self._exc:
            raise self._exc
        return self._val


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

    def _mk_request(self, routing_key: str, func: str, *args) -> SyncAmqpResult:
        _id = str(uuid4())
        result = SyncAmqpResult(timeout=self.request_timeout)
        payload = AmqpRequest(
            id=_id,
            api_keys=self.api_keys or {},
            response_routing_key=self.read_queue_name,
            func=func,
            args=args
        )

        kwargs = {
            'routing_key': routing_key,
        }
        if self.high_priority:
            kwargs['priority'] = 1

        self.pending_async_results[_id] = result
        # declare incoming queue (if not yet done) before making of request, otherwise response may be lost
        self.declare_read_queue(**self._read_queue_kwargs)
        self.publish(astuple(payload), **kwargs)
        return result


class AmqpApiWithBlockingListener(BaseAmqpApiClient):
    """
    Synchronous (blocking), gevent-free version of GatewayApi. Listens messages in the same thread.
    """

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._lock = Lock()

    def _process_async_result(self, body: list, message: Message):
        """
        Compares length of pending requests list before and after response processing.
        Stops queue listening if it was 1 and became 0 (that means desired response has been received)
        """
        before = len(self.pending_async_results)
        super()._process_async_result(body, message)
        after = len(self.pending_async_results)

        if before and not after:
            raise StopIteration

    def _mk_request(self, routing_key: str, func: str, *args) -> SyncAmqpResult:
        with self._lock:
            ret = super()._mk_request(routing_key, func, *args)

            # listen to queue and interrupt after first message
            try:
                self.listen(**self._read_queue_kwargs)
            except StopIteration:
                # desired answer should have been received
                return ret
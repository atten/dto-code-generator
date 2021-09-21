JSON_PAYLOAD = t.Union[dict, str, int, float, list]


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
        func_name_verbose = '{}.{}'.format(func.__self__.__name__, func.__name__)
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


class BaseJsonApiClient:
    base_url = ''

    def __init__(self, base_url: str = '', logger=None):
        if base_url:
            self.base_url = base_url
        self.logger = logger

    def get_base_url(self) -> str:
        return self.base_url

    def _fetch(
        self,
        url: str,
        method: str = 'get',
        query_params: t.Optional[dict] = None,
        payload: t.Optional[JSON_PAYLOAD] = None,
    ) -> JSON_PAYLOAD:
        """
        Retrieve JSON response from remote API request.

        Repeats request in case of network errors.

        :param url: target url (relative to base url)
        :param method: HTTP verb, e.g. get/post
        :param query_params: key-value arguments like ?param1=11&param2=22
        :param payload: JSON-like HTTP body
        :return: decoded JSON from server
        """
        if payload:
            payload = json.dumps(payload).encode('utf8')

        request = Request(
            url=self._get_full_url(url, query_params),
            method=method.upper(),
            headers={'content-type': 'application/json'},
            data=payload,
        )

        try:
            return failsafe_call(
                self._mk_request,
                args=(request,),
                exceptions=(URLError,),  # filters out connection errors, HTTP 400, 500 etc
                logger=self.logger,
                max_attempts=5,
                on_transitional_fail=lambda exc, info: sleep(2)
            )
        except Exception as e:
            raise RuntimeError(f'Failed to connect {request.full_url}: {e}')

    @classmethod
    def _mk_request(cls, request: Request) -> JSON_PAYLOAD:
        response = urlopen(request)
        if response.status >= 400:
            raise HTTPError(request.full_url, response.status, response.read(), request.headers, None)
        bytes_data = response.read()

        if 'json' in response.headers.get('content-type', ''):
            bytes_data = bytes_data.strip()
            if not bytes_data:
                return ''
            return json.loads(bytes_data.decode('utf-8'))
        return bytes_data

    def _get_full_url(self, url: str, query_params: t.Optional[dict] = None) -> str:
        if self.base_url:
            url = urljoin(self.base_url, url)

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
        if isinstance(value, (list, tuple, set)):
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
            return func(value, many=many)

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
        value = value.isoformat()
        if value.endswith('+00:00'):
            value = value[:-6] + 'Z'
        return value

    @classmethod
    def _serialize_timedelta(cls, value: timedelta) -> str:
        return '{seconds}s'.format(seconds=int(value.total_seconds()))

    def _deserialize(self, raw_data: JSON_PAYLOAD, data_class: t.Type, many: bool = False) -> t.Any:
        if raw_data == b'':
            return None

        # pick built-in deserializer if specified for class
        method_name = '_deserialize_{type}'.format(type=data_class.__name__.lower())
        if hasattr(self, method_name):
            method = getattr(self, method_name)
            if many:
                return list(map(method, raw_data))
            return method(raw_data)

        try:
            # use marshmallow in other cases
            schema = marshmallow_dataclass.class_schema(data_class)()
        except TypeError:
            # fallback to default constructor
            return data_class(raw_data)

        return schema.load(raw_data, many=many)

    @classmethod
    def _deserialize_datetime(cls, raw: str) -> datetime:
        if raw.endswith('Z'):
            raw = raw[:-1] + '+00:00'
        return datetime.fromisoformat(raw)
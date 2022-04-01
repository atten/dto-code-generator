JSON_PAYLOAD = t.Union[dict, str, int, float, list]
RESPONSE_BODY = [str, io.IOBase]


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


class BaseSchema(marshmallow.Schema):
    class Meta:
        # allow backward-compatible changes when new fields have added (simply ignore them)
        unknown = marshmallow.EXCLUDE


class BaseJsonApiClient:
    base_url = ''
    default_max_retries = int(os.environ.get('API_CLIENT_MAX_RETRIES', 5))
    default_retry_timeout = float(os.environ.get('API_CLIENT_RETRY_TIMEOUT', 3))
    use_response_streaming = bool(int(os.environ.get('API_CLIENT_USE_STREAMING', 1)))

    def __init__(
        self,
        base_url: str = '',
        logger=None,
        max_retries: int = default_max_retries,
        retry_timeout: float = default_retry_timeout,
    ):
        """
        Remote API client constructor.

        :param base_url: protocol://url[:port]
        :param logger: logger instance
        :param max_retries: number of connection attempts before RuntimeException raise
        :param retry_timeout: seconds between attempts
        """
        if base_url:
            self.base_url = base_url
        self.logger = logger
        self.pool = urllib3.PoolManager(retries=False)
        self.max_retries = max_retries
        self.retry_timeout = retry_timeout

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
        headers = dict()
        if payload:
            payload = json.dumps(payload).encode('utf8')
            headers['content-type'] = 'application/json'

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
            raise RuntimeError(f'Failed to {method} {full_url}: {error_verbose}')

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
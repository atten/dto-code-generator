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
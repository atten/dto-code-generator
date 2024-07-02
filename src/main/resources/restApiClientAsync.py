JSON_PAYLOAD = t.Union[dict, str, int, float, list]
RESPONSE_BODY = JSON_PAYLOAD


class BaseSchema(marshmallow.Schema):
    class Meta:
        # allow backward-compatible changes when new fields have added (simply ignore them)
        unknown = marshmallow.EXCLUDE


class BaseJsonApiClientAsync:
    base_url = ''
    default_max_retries = int(os.environ.get('API_CLIENT_MAX_RETRIES', 5))
    default_retry_timeout = float(os.environ.get('API_CLIENT_RETRY_TIMEOUT', 3))
    default_user_agent = os.environ.get('API_CLIENT_USER_AGENT')
    use_response_streaming = bool(int(os.environ.get('API_CLIENT_USE_STREAMING', 0)))   # disabled for async client (not implemented yet)

    @typechecked
    def __init__(
        self,
        base_url: str = '',
        logger: t.Union[logging.Logger, t.Callable[[str], None]] = None,
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
        self.max_retries = max_retries
        self.retry_timeout = retry_timeout
        self.user_agent = user_agent
        self.headers = headers

    def get_base_url(self) -> str:
        return self.base_url

    async def _fetch(
        self,
        url: str,
        method: str = 'get',
        query_params: t.Optional[dict] = None,
        headers: t.Optional[dict] = None,
        payload: t.Optional[JSON_PAYLOAD] = None,
    ) -> JSON_PAYLOAD:
        """
        Retrieve JSON response from remote API request.

        Repeats request in case of network errors.

        :param url: target url (relative to base url)
        :param method: HTTP verb, e.g. get/post
        :param query_params: key-value arguments like ?param1=11&param2=22
        :param headers: dict of HTTP headers
        :param payload: JSON-like HTTP body
        :return: decoded JSON from server
        """
        full_url = self._get_full_url(url, query_params)
        if not headers:
            headers = self.headers.copy() if self.headers else dict()
        if payload:
            headers['content-type'] = 'application/json'
        if self.user_agent:
            headers['user-agent'] = self.user_agent

        try:
            return await failsafe_call_async(
                self._mk_request,
                kwargs=dict(
                    full_url=full_url,
                    method=method,
                    headers=headers,
                    payload=payload,
                ),
                exceptions=(aiohttp.ClientConnectorError, ConnectionRefusedError),
                logger=self.logger,
                max_attempts=self.max_retries,
                on_transitional_fail=lambda exc, info: asyncio.sleep(self.retry_timeout)
            )
        except Exception as e:
            raise RuntimeError(f'Failed to {method} {full_url}: {e}') from e

    @classmethod
    async def _mk_request(cls, full_url: str, method: str, payload: t.Optional[dict], headers: t.Optional[dict]) -> JSON_PAYLOAD:
        async with aiohttp.request(
            url=full_url,
            method=method,
            headers=headers,
            json=payload,
        ) as response:
            response.raise_for_status()
            return await response.json()

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
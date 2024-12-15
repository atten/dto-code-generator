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
        use_debug_curl: bool,
        use_response_streaming: bool,
    ):
        self._base_url = base_url
        self._logger = logger
        self._max_retries = max_retries
        self._retry_timeout = retry_timeout
        self._user_agent = user_agent
        self._headers = headers
        self._use_debug_curl = use_debug_curl

    async def fetch(
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
        headers = self._headers.copy() if self._headers else dict()
        if payload is not None:
            headers['content-type'] = 'application/json'
        if self._user_agent:
            headers['user-agent'] = self._user_agent

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
                    body=payload,
                )
                raise RuntimeError(f'Failed to {curl_cmd}: {e}') from e
            raise RuntimeError(f'Failed to {method} {full_url}: {e}') from e

    @classmethod
    async def _mk_request(cls, full_url: str, method: str, payload: t.Optional[JSON_PAYLOAD], headers: t.Optional[dict]) -> RESPONSE_BODY:
        async with aiohttp.request(
            url=full_url,
            method=method,
            headers=headers,
            json=payload,
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
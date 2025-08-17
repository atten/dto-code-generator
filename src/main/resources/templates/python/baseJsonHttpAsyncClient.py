JSON_PAYLOAD = t.Union[dict, str, int, float, list]
RESPONSE_BODY = JSON_PAYLOAD


class BaseJsonHttpAsyncClient:
    def __init__(
        self,
        base_url: str,
        logger: t.Union[logging.Logger, t.Callable[[str], None], None],
        max_retries: int,
        retry_timeout: float,
        user_agent: str | None,
        headers: dict[str, str] | None,
        use_response_streaming: bool,
        use_debug_curl: bool,
        request_kwargs: dict,
        connection_pool_kwargs: dict,
        exception_class: t.Type[Exception],
    ):
        self._base_url = base_url
        self._logger = logger
        self._max_retries = max_retries
        self._retry_timeout = retry_timeout
        self._user_agent = user_agent
        self._headers = headers
        self._use_debug_curl = use_debug_curl
        self._request_kwargs = request_kwargs
        self._exception_class = exception_class

    async def fetch(
        self,
        url: str,
        method: str = 'get',
        query_params: dict | None = None,
        json_body: JSON_PAYLOAD | None = None,
        form_fields: dict[str, str] | None = None,
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
                exceptions=(aiohttp.ClientConnectorError, aiohttp.ClientResponseError, ConnectionRefusedError),
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
                raise self._exception_class(f'Failed to {curl_cmd}: {e}') from e
            raise self._exception_class(f'Failed to {method} {full_url}: {e}') from e

    @classmethod
    async def _mk_request(cls, full_url: str, method: str, body: JSON_PAYLOAD | None, headers: dict | None) -> RESPONSE_BODY:
        async with aiohttp.request(
            url=full_url,
            method=method,
            headers=headers,
            json=body,
        ) as response:
            response.raise_for_status()

            if 'json' in response.headers.get('content-type', ''):
                return await response.json()

            return await response.text()

    def _get_full_url(self, url: str, query_params: dict | None = None) -> str:
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
JSON_PAYLOAD = t.Union[dict, str, int, float, list]
RESPONSE_BODY = [str, io.IOBase]


class BaseJsonHttpClient:
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
        connection_pool_kwargs.update(retries=False)

        self._pool = urllib3.PoolManager(**connection_pool_kwargs)
        self._base_url = base_url
        self._logger = logger
        self._max_retries = max_retries
        self._retry_timeout = retry_timeout
        self._user_agent = user_agent
        self._headers = headers
        self._use_response_streaming = use_response_streaming
        self._use_debug_curl = use_debug_curl
        self._request_kwargs = request_kwargs

    def fetch(
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
            payload = json.dumps(payload).encode('utf8')
            headers['content-type'] = 'application/json'
        if self._user_agent:
            headers['user-agent'] = self._user_agent

        request_kwargs = self._request_kwargs.copy()
        request_kwargs.update(
            url=full_url,
            method=method,
            headers=headers,
            body=payload,
        )

        try:
            return failsafe_call(
                self._mk_request,
                kwargs=request_kwargs,
                exceptions=(urllib3.exceptions.HTTPError,),  # include connection errors, HTTP >= 400
                logger=self._logger,
                max_attempts=self._max_retries,
                on_transitional_fail=lambda exc, info: sleep(self._retry_timeout)
            )
        except Exception as e:
            error_verbose = str(e)
            if ' at 0x' in error_verbose:
                # reduce noise in error description, e.g. in case of NewConnectionError
                error_verbose = error_verbose.split(':', maxsplit=1)[-1].strip()
            if self._use_debug_curl:
                curl_cmd = build_curl_command(
                    url=full_url,
                    method=method,
                    headers=headers,
                    body=payload,
                )
                raise RuntimeError(f'Failed to {curl_cmd}: {error_verbose}') from e

            raise RuntimeError(f'Failed to {method} {full_url}: {error_verbose}') from e

    def _mk_request(self, *args, **kwargs) -> RESPONSE_BODY:
        response = self._pool.request(*args, **kwargs, preload_content=False)
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
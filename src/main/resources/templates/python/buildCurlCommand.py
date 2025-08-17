def build_curl_command(url: str, method: str, headers: dict[str, str], body: str) -> str:
    """
    >>> build_curl_command('https://example.com', 'get', {}, '')
    'curl "https://example.com"'

    >>> build_curl_command('https://example.com?param1=value1&param2=value2', 'post', {'content-type': 'application/json'}, '{"foo": "bar"}')
    'curl "https://example.com?param1=value1&param2=value2" -X POST -H "content-type: application/json" -d "{\"foo\": \"bar\"}"'
    """
    method = method.upper()

    if method != 'GET':
        method = f' -X {method}'
    else:
        method = ''

    headers = ''.join(f' -H "{k}: {v}"' for k, v in headers.items())

    if body:
        body = body.replace('"', '\"')
        body = f' -d "{body}"'
    else:
        body = ''

    return f'curl "{url}"{method}{headers}{body}'
def _get_func_name_verbose(func: t.Callable) -> str:
    if hasattr(func, '__self__'):
        if hasattr(func.__self__, '__name__'):
            return '{}.{}'.format(func.__self__.__name__, func.__name__)
        return '{}.{}'.format(func.__self__.__class__.__name__, func.__name__)
    elif hasattr(func, '__name__'):
        return func.__name__
    return repr(func)


async def failsafe_call_async(
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
    func_name_verbose = _get_func_name_verbose(func)

    try:
        return await func(*args, **kwargs)
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
            await on_transitional_fail(e, dict(max_attempts=max_attempts, attempt=attempt))

        return await failsafe_call_async(
            func,
            exceptions,
            args,
            kwargs,
            logger,
            attempt + 1,
            max_attempts,
            on_transitional_fail
        )

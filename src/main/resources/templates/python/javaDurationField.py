class JavaDurationField(marshmallow.fields.Field):

    def _deserialize(self, value, attr, data, **kwargs):
        try:
            return str_java_duration_to_timedelta(value)
        except ValueError as error:
            raise marshmallow.ValidationError(str(error)) from error

    def _serialize(self, value: timedelta | None, attr: str, obj, **kwargs):
        if value is None:
            return None
        return timedelta_to_java_duration(value) if value else "PT0S"


def str_java_duration_to_timedelta(duration: str) -> timedelta:
    """
    >>> str_java_duration_to_timedelta('PT5S')
    datetime.timedelta(seconds=5)

    >>> str_java_duration_to_timedelta('PT10H59S')
    datetime.timedelta(seconds=36059)

    >>> str_java_duration_to_timedelta('PT0H5M')
    datetime.timedelta(seconds=300)
    """
    groups = re.findall(r'PT(\d+H)?(\d+M)?([\d.]+S)?', duration)[0]
    if not groups:
        raise ValueError('Invalid duration: %s' % duration)

    hours, minutes, seconds = groups

    hours = int((hours or '0H').rstrip('H'))
    minutes = int((minutes or '0M').rstrip('M'))
    seconds = float((seconds or '0S').rstrip('S'))

    return timedelta(hours=hours, minutes=minutes, seconds=seconds)


def timedelta_to_java_duration(delta: timedelta) -> str:
    """
    Converts a timedelta to java duration string format
    Milliseconds are discarded

    >>> timedelta_to_java_duration(timedelta(minutes=15))
    'PT900S'

    >>> timedelta_to_java_duration(timedelta(days=1, minutes=21, seconds=35))
    'PT87695S'

    >>> timedelta_to_java_duration(timedelta(microseconds=123456))
    'PT0S'
    """
    seconds = delta.total_seconds()
    return 'PT{}S'.format(int(seconds))

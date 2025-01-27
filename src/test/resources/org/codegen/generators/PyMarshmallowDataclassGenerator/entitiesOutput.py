# Auto-generated by DTO-Codegen TEST_VERSION, do not edit
# flake8: noqa
from dataclasses import dataclass
from dataclasses import field
from datetime import datetime
from datetime import timedelta
import marshmallow
import marshmallow_dataclass
import re
import typing as t


class BaseSchema(marshmallow.Schema):
    class Meta:
        # allow backward-compatible changes when new fields have added (simply ignore them)
        unknown = marshmallow.EXCLUDE


class JavaDurationField(marshmallow.fields.Field):

    def _deserialize(self, value, attr, data, **kwargs):
        try:
            return str_java_duration_to_timedelta(value)
        except ValueError as error:
            raise marshmallow.ValidationError(str(error)) from error

    def _serialize(self, value: t.Optional[timedelta], attr: str, obj, **kwargs):
        if value is None:
            return None
        return timedelta_to_java_duration(value) if value else "PT0S"


def str_java_duration_to_timedelta(duration: str) -> timedelta:
    """
    :param duration: string duration:'PT5S', 'PT10H59S' etc
    :return: timedelta()
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


ENUM_VALUE_VALUE_1 = "value 1"
ENUM_VALUE_VALUE_2 = "value 2"
ENUM_VALUE_VALUE_3 = "value 3"
ENUM_VALUES = [ENUM_VALUE_VALUE_1, ENUM_VALUE_VALUE_2, ENUM_VALUE_VALUE_3]


@dataclass
class BasicDto:
    timestamp: datetime = field(metadata=dict(marshmallow_field=marshmallow.fields.DateTime()))
    duration: timedelta = field(metadata=dict(marshmallow_field=JavaDurationField()))
    enum_value: str = field(metadata=dict(marshmallow_field=marshmallow.fields.String(validate=[marshmallow.fields.validate.OneOf(ENUM_VALUES)])))
    # short description
    # very long description lol
    documented_value: float = field(metadata=dict(marshmallow_field=marshmallow.fields.Float(data_key="customName")))
    list_value: list[int] = field(metadata=dict(marshmallow_field=marshmallow.fields.List(marshmallow.fields.Integer())))
    optional_value: float = field(metadata=dict(marshmallow_field=marshmallow.fields.Float()), default=0)
    nullable_value: t.Optional[bool] = field(metadata=dict(marshmallow_field=marshmallow.fields.Boolean(allow_none=True)), default=None)
    optional_list_value: list[int] = field(metadata=dict(marshmallow_field=marshmallow.fields.List(marshmallow.fields.Integer())), default_factory=list)


@dataclass
class AdvancedDto:
    """
    entity with all-singing all-dancing properties
    """
    a: int = field(metadata=dict(marshmallow_field=marshmallow.fields.Integer()))
    b: int = field(metadata=dict(marshmallow_field=marshmallow.fields.Integer()))

    def __post_init__(self):
        if not(self.a < self.b):
            raise marshmallow.ValidationError('a must be < b')
        if not(self.a >= 0):
            raise marshmallow.ValidationError('a must be >= 0')

    @property
    def sum(self) -> int:
        return self.a + self.b


@dataclass
class ContainerDto:
    """
    entity with containers
    """
    basic_single: BasicDto = field(metadata=dict(marshmallow_field=marshmallow.fields.Nested(marshmallow_dataclass.class_schema(BasicDto, base_schema=BaseSchema), data_key="basic")))
    basic_list: t.Optional[list[BasicDto]] = field(metadata=dict(marshmallow_field=marshmallow.fields.List(marshmallow.fields.Nested(marshmallow_dataclass.class_schema(BasicDto, base_schema=BaseSchema)), allow_none=True, data_key="basics")))


__all__ = [
    "AdvancedDto",
    "BasicDto",
    "ContainerDto",
    "ENUM_VALUES",
    "ENUM_VALUE_VALUE_1",
    "ENUM_VALUE_VALUE_2",
    "ENUM_VALUE_VALUE_3",
]

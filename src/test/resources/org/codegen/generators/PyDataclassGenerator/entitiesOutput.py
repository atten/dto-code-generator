# Generated by DTO-Codegen

from dataclasses import dataclass
from dataclasses import field
from datetime import datetime
import typing as t


@dataclass
class BasicDTO:
    timestamp: datetime = field()
    enum_value: str = field()
    # short description
    # very long description lol
    documented_value: float = field()
    list_value: list[int] = field()
    optional_value: float = 0
    nullable_value: t.Optional[bool] = None
    optional_list_value: list[int] = field(default_factory=list)


@dataclass
class AdvancedDTO:
    """
    entity with all-singing all-dancing properties
    """
    a: int = field()
    b: int = field()

    @property
    def sum(self) -> int:
        return self.a + self.b


@dataclass
class ContainerDTO:
    """
    entity with containers
    """
    basic_single: BasicDTO = field()
    basic_list: t.Optional[list[BasicDTO]] = field()


__all__ = [
    "AdvancedDTO",
    "BasicDTO",
    "ContainerDTO",
]

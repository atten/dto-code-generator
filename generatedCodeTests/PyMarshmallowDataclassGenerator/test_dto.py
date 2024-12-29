import pytest
from marshmallow.exceptions import ValidationError


def test_import():
    from generated.dto import AdvancedDto  # noqa


def test_property():
    from generated.dto import AdvancedDto

    dto = AdvancedDto(a=5, b=10)
    assert dto.sum == 15


def test_validators():
    from generated.dto import AdvancedDto

    with pytest.raises(ValidationError):
        AdvancedDto(a=-5, b=10)

    with pytest.raises(ValidationError):
        AdvancedDto(a=55, b=10)

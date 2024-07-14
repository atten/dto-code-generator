import pytest
from marshmallow.exceptions import ValidationError


def test_import():
    from generated.dto import AdvancedDTO  # noqa


def test_property():
    from generated.dto import AdvancedDTO

    dto = AdvancedDTO(a=5, b=10)
    assert dto.sum == 15


def test_validators():
    from generated.dto import AdvancedDTO

    with pytest.raises(ValidationError):
        AdvancedDTO(a=-5, b=10)

    with pytest.raises(ValidationError):
        AdvancedDTO(a=55, b=10)

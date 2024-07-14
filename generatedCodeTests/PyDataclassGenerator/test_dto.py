import pytest


def test_import():
    from generated.dto import AdvancedDTO  # noqa


def test_property():
    from generated.dto import AdvancedDTO

    dto = AdvancedDTO(a=5, b=10)
    assert dto.sum == 15

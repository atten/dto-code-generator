import types


def test_import():
    from generated.api import TestApiClient

def test_get_request():
    from generated.api import TestApiClient, BasicDTO

    api = TestApiClient(
        base_url='http://mock_server',
    )

    result = api.get_basic_dto_list()
    assert isinstance(result, types.GeneratorType)
    assert isinstance(next(result), BasicDTO)

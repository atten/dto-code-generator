# Yet another Code Generator

[![pipeline](https://gitlab.com/atten0/dto-code-generator/badges/master/pipeline.svg)](https://gitlab.com/atten0/dto-code-generator/-/pipelines)
[![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg)](http://www.jacoco.org/jacoco)

Generate pretty API clients from OpenApi specs with ease.

## Install

### Docker image

```
docker image pull registry.gitlab.com/atten0/dto-code-generator:master
```

Other image versions:

https://gitlab.com/atten0/dto-code-generator/container_registry

### Binary format (linux/x86)

Download and extract archive into current dir:

```
wget -qO- https://github.com/atten/dto-code-generator/releases/download/v1.3.0/dto-codegen-1.3.0.zip | busybox unzip -
```

Other versions lists:

- https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/build/distributions/?job=publish-distro

- https://github.com/atten/dto-code-generator/releases

## Usage

    ./dto-codegen [options] files/directories to parse

Options:

    -t, --target
    Target implementation
    Possible Values: [KT_DATACLASS, KT_SERIALIZABLE_DATACLASS, KT_INTERFACE, PY_DJANGO_MODEL, PY_API_CLIENT, PY_API_ASYNC_CLIENT, PY_AMQP_BLOCKING_CLIENT, PY_AMQP_GEVENT_CLIENT, PY_MARSHMALLOW_DATACLASS, PY_DATACLASS]
    
    -n, --name
    Generated class name (inferred from input files if not specified)
    Default: <empty string>
    
    --include-path
    Include only paths containing given strings
    Default: []
    
    --exclude-path
    Do not include paths containing given strings
    Default: []
    
    -p, --prefixed
    If enabled, add prefix to all fields
    Default: false
    
    --help
    Show help usage and exit
    
    --version, -v
    Display version and exit

## Examples

TODO

## Avaliable generators


| Type | Language/Framework | Serialization         | Dependencies                                                                            | Name                    | Example                                                                                                                                       | Coverage                                                                                                                                                                                                                                                                                                                      |
|------|--------------------|-----------------------|-----------------------------------------------------------------------------------------|-------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| DAO  | Kotlin             | kotlinx.serialization |                                                                                         | KtSerializableDataclass | [entitiesOutput.kt](src/test/resources/org/codegen/generators/KtSerializableDataclassGenerator/entitiesOutput.kt)                             | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-KtSerializableDataclassGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/KtSerializableDataclassGenerator/htmlcov?job=run-tests-KtSerializableDataclassGenerator) |
| DAO  | Kotlin             | Jackson               |                                                                                         | KtSerializableDataclass | [entitiesOutputJacksonEnabled.kt](src/test/resources/org/codegen/generators/KtSerializableDataclassGenerator/entitiesOutputJacksonEnabled.kt) | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-KtSerializableDataclassGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/KtSerializableDataclassGenerator/htmlcov?job=run-tests-KtSerializableDataclassGenerator) |
| HTTP | Python3            | Marshmallow           | [requirements.txt](generatedCodeTests/PyApiClientGenerator/requirements.txt)            | PyApiClient             | [endpointsOutput.py](src/test/resources/org/codegen/generators/PyApiClientGenerator/endpointsOutput.py)                                       | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyApiClientGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyApiClientGenerator/htmlcov?job=run-tests-PyApiClientGenerator)                                     |
| HTTP | Python3 (async)    | Marshmallow           | [requirements.txt](generatedCodeTests/PyApiAsyncClientGenerator/requirements.txt)       | PyApiClientAsync        | [endpointsOutput.py](src/test/resources/org/codegen/generators/PyApiAsyncClientGenerator/endpointsOutput.py)                                  | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyApiAsyncClientGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyApiAsyncClientGenerator/htmlcov?job=run-tests-PyApiAsyncClientGenerator)                      |
| DAO  | Python3            | Dataclass             | -                                                                                       | PyDataclass             | [entitiesOutput.py](src/test/resources/org/codegen/generators/PyDataclassGenerator/entitiesOutput.py)                                         | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyDataclassGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyDataclassGenerator/htmlcov?job=run-tests-PyDataclassGenerator)                                     |
| DAO  | Python3            | Marshmallow           | [requirements.txt](generatedCodeTests/PyMarshmallowDataclassGenerator/requirements.txt) | PyMarshmallowDataclass  | [entitiesOutput.py](src/test/resources/org/codegen/generators/PyMarshmallowDataclassGenerator/entitiesOutput.py)                              | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyMarshmallowDataclassGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyMarshmallowDataclassGenerator/htmlcov?job=run-tests-PyMarshmallowDataclassGenerator)    |
| ORM  | Python3 + Django   | -                     | [requirements.txt](generatedCodeTests/PyDjangoModelGenerator/requirements.txt)          | PyDjangoModel           | [entitiesOutput.py](src/test/resources/org/codegen/generators/PyDjangoModelGenerator/entitiesOutput.py)                                       | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyDjangoModelGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyDjangoModelGenerator/app/migrations?job=run-tests-PyDjangoModelGenerator)                        |
| AMQP | Python3            | Marshmallow           |                                                                                         | PyAmqpBlockingClient    | [endpointsOutput.py](src/test/resources/org/codegen/generators/PyAmqpBlockingClientGenerator/endpointsOutput.py)                              | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyAmqpBlockingClientGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyAmqpBlockingClientGenerator/htmlcov?job=run-tests-PyAmqpBlockingClientGenerator)          |
| AMQP | Python3 + Gevent   | Marshmallow           |                                                                                         | PyAmqpGeventClient      | [endpointsOutput.py](src/test/resources/org/codegen/generators/PyAmqpGeventClientGenerator/endpointsOutput.py)                                | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyAmqpGeventClientGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyAmqpGeventClientGenerator/htmlcov?job=run-tests-PyAmqpGeventClientGenerator)                |

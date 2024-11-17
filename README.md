# dto-code-generator

[![pipeline](https://gitlab.com/atten0/dto-code-generator/badges/master/pipeline.svg)](https://gitlab.com/atten0/dto-code-generator/-/pipelines)
[![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg)](http://www.jacoco.org/jacoco)

Console tool which generates language-specific data classes, validators and API clients from JSON-like schema

## Avaliable generators


| Name                    | Coverage                                                                                                                                                                                                                                                                                                                      |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| PyApiClient             | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyApiClientGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyApiClientGenerator/htmlcov?job=run-tests-PyApiClientGenerator)                                     |
| PyApiClientAsync        | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyApiAsyncClientGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyApiAsyncClientGenerator/htmlcov?job=run-tests-PyApiAsyncClientGenerator)                      |
| PyAmqpBlockingClient    | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyAmqpBlockingClientGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyAmqpBlockingClientGenerator/htmlcov?job=run-tests-PyAmqpBlockingClientGenerator)          |
| PyAmqpGeventClient      | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyAmqpGeventClientGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyAmqpGeventClientGenerator/htmlcov?job=run-tests-PyAmqpGeventClientGenerator)                |
| PyDataclass             | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyDataclassGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyDataclassGenerator/htmlcov?job=run-tests-PyDataclassGenerator)                                     |
| PyMarshmallowDataclass  | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyMarshmallowDataclassGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyMarshmallowDataclassGenerator/htmlcov?job=run-tests-PyMarshmallowDataclassGenerator)    |
| PyDjangoModel           | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyDjangoModelGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyDjangoModelGenerator/app/migrations?job=run-tests-PyDjangoModelGenerator)                        |
| KtSerializableDataclass | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-KtSerializableDataclassGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/KtSerializableDataclassGenerator/htmlcov?job=run-tests-KtSerializableDataclassGenerator) |


## Install

### Binary format (zip, tar.gz)

#### Stable version

Download and extract archive into current dir:

```
wget -qO- https://github.com/atten/dto-code-generator/releases/download/v1.1.0/dto-codegen-1.1.0.zip | busybox unzip -
```

#### Other versions

- https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/build/distributions/?job=publish-distro

- https://github.com/atten/dto-code-generator/releases


### Docker image

#### Stable version

```
docker image pull registry.gitlab.com/atten0/dto-code-generator:release_1.1.0
```

#### Other versions

https://gitlab.com/atten0/dto-code-generator/container_registry
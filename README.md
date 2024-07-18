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

### Stable version (1.0.0)

Download and extract archive into current dir:

```
wget -qO- https://github.com/atten/dto-code-generator/releases/download/v1.0.0/dto-codegen-1.0.0.zip | busybox unzip -
```

### Latest version

#### Zip and tar.gz
https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/build/distributions/?job=publish-distro

#### Docker image
```
docker image pull registry.gitlab.com/atten0/dto-code-generator:master
```

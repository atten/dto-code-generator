# dto-code-generator

[![pipeline](https://gitlab.com/atten0/dto-code-generator/badges/master/pipeline.svg)](https://gitlab.com/atten0/dto-code-generator/-/pipelines)
[![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg)](http://www.jacoco.org/jacoco)

Console tool which generates language-specific data classes, validators and API clients from JSON-like schema

## Avaliable generators


| Name                    | Coverage                                                                                                                                                           |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| PyApiClient             | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyApiClientGenerator)](https://gitlab.com/atten0/dto-code-generator/-/jobs/artifacts/master/browse/generatedCodeTests/PyApiClientGenerator/htmlcov?job=run-tests-PyApiClientGenerator)            |
| PyApiClientAsync        | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyApiClientAsyncGenerator)](http://www.jacoco.org/jacoco)       |
| PyAmqpBlockingClient    | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyAmqpBlockingClientGenerator)](http://www.jacoco.org/jacoco)   |
| PyAmqpGeventClient      | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyAmqpGeventClientGenerator)](http://www.jacoco.org/jacoco)     |
| PyDataclass             | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyDataclassGenerator)](http://www.jacoco.org/jacoco)            |
| PyMarshmallowDataclass  | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyMarshmallowDataclassGenerator)](http://www.jacoco.org/jacoco) |
| PyDjangoModel           | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-PyDjangoModelGenerator)](http://www.jacoco.org/jacoco)          |
| KtSerializableDataclass | [![coverage](https://gitlab.com/atten0/dto-code-generator/badges/master/coverage.svg?job=run-tests-KtSerializableDataclassGenerator)](http://www.jacoco.org/jacoco)          |
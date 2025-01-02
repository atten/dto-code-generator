mkdir -p generated/
cp ../../src/test/resources/org/codegen/generators/PyDjangoModelGenerator/entitiesOutput.py generated/entities_models.py
cp ../../src/test/resources/org/codegen/generators/PyDjangoModelGenerator/openApiOutput.py generated/openapi_models.py
rm -rf entities/migrations openapi/migrations
docker compose --progress quiet up --build --abort-on-container-exit

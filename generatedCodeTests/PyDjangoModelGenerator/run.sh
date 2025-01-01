mkdir -p generated/
cp ../../src/test/resources/org/codegen/generators/PyDjangoModelGenerator/entitiesOutput.py generated/entities_models.py
rm -rf entities/migrations
docker compose --progress quiet up --build --abort-on-container-exit

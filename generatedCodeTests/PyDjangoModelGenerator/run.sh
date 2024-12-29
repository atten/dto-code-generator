mkdir -p generated/
cp -v ../../src/test/resources/org/codegen/generators/PyDjangoModelGenerator/entitiesOutput.py generated/models.py
rm -rf app/migrations
docker compose up --build --abort-on-container-exit

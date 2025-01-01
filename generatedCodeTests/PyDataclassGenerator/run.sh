mkdir -p generated/
cp ../../src/test/resources/org/codegen/generators/PyDataclassGenerator/entitiesOutput.py generated/dto.py
docker compose --progress quiet up --build --abort-on-container-exit

mkdir -p generated/
cp ../../src/test/resources/org/codegen/generators/PyMarshmallowDataclassGenerator/entitiesOutput.py generated/dto.py
docker compose --progress quiet up --build --abort-on-container-exit

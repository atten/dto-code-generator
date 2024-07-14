mkdir -p generated/
cp -v ../../src/test/resources/org/codegen/generators/PyMarshmallowDataclassGenerator/entitiesOutput.py generated/dto.py
docker-compose up --build --abort-on-container-exit

mkdir -p generated/
cp -v ../../src/test/resources/org/codegen/generators/PyApiClientGenerator/endpointsOutput.py generated/api.py
docker compose up --build --abort-on-container-exit

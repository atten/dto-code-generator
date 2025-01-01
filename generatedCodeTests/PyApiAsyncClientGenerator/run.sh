mkdir -p generated/
cp ../../src/test/resources/org/codegen/generators/PyApiAsyncClientGenerator/endpointsOutput.py generated/api.py
docker compose --progress quiet up --build --abort-on-container-exit

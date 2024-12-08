mkdir -p generated/
cp -v ../../src/test/resources/org/codegen/generators/PyApiAsyncClientGenerator/endpointsOutput.py generated/api.py
docker compose up --build --abort-on-container-exit

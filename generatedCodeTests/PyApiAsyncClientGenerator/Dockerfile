ARG IMAGE
FROM $IMAGE

WORKDIR /app

ADD requirements.txt requirements.txt
RUN pip install pytest-cov pytest-asyncio
RUN pip install --no-cache-dir -r requirements.txt

ADD . .

CMD pytest --doctest-modules --cov=. --cov-report term --cov-report html

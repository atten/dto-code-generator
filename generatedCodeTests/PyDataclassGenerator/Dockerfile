ARG IMAGE
FROM $IMAGE

WORKDIR /app

RUN pip install pytest-cov

ADD . .

CMD pytest --doctest-modules --cov=. --cov-report term --cov-report html

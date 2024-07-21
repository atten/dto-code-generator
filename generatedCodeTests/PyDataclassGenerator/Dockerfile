FROM python:3.10-alpine

WORKDIR /app

RUN pip install pytest-cov

ADD . .

CMD pytest --doctest-modules --cov=. --cov-report term --cov-report html

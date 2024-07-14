FROM python:3.10-alpine

WORKDIR /app

ADD requirements.txt requirements.txt
RUN pip install pytest-cov
RUN pip install --no-cache-dir -r requirements.txt

ADD . .

CMD pytest --cov=. --cov-report term --cov-report html

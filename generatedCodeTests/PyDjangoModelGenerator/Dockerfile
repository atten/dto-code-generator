FROM python:3.10-alpine

WORKDIR /app

ADD requirements.txt requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

ADD . .

CMD ./manage.py makemigrations app

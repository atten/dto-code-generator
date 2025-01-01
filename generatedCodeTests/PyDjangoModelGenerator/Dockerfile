FROM python:3.10-alpine

ADD requirements.txt requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

RUN django-admin startproject testproject

WORKDIR /testproject

RUN sed -i 's/INSTALLED_APPS = \[/INSTALLED_APPS = \["entities",/g' testproject/settings.py

ADD . .

CMD ./manage.py makemigrations entities

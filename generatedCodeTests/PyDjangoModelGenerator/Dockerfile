ARG IMAGE
FROM $IMAGE

ADD requirements.txt requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

RUN django-admin startproject testproject

WORKDIR /testproject

RUN sed -i 's/INSTALLED_APPS = \[/INSTALLED_APPS = \["entities","openapi",/g' testproject/settings.py

ADD . .

CMD rm -rf entities/migrations/* openapi/migrations/* && ./manage.py makemigrations entities openapi

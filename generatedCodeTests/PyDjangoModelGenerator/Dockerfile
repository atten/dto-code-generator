ARG IMAGE
FROM $IMAGE

ADD requirements.txt requirements.txt
RUN pip install pytest-django pytest-cov
RUN pip install --no-cache-dir -r requirements.txt

RUN django-admin startproject testproject
ENV DJANGO_SETTINGS_MODULE testproject.settings

WORKDIR /testproject

ADD . .

RUN cat testproject/settings.py settings_addon.py > testproject/settings1.py && \
    mv testproject/settings1.py testproject/settings.py

CMD rm -rf entities/migrations/* openapi/migrations/* && \
    ./manage.py makemigrations entities openapi && \
    sleep 5 && \
    ./manage.py migrate && \
    pytest --doctest-modules --cov=generated --cov-report term --cov-report html

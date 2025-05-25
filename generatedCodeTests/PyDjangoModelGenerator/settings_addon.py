INSTALLED_APPS = [
    'entities',
    'openapi',
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
]

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql',
        'HOST': 'postgres',
        'PORT': '5432',
        'NAME': 'DjangoModelGenerator',
        'USER': 'DjangoModelGenerator',
        'PASSWORD': 'DjangoModelGenerator',
    }
}

# Auto-generated by DTO-Codegen TEST_VERSION, do not edit

from django.contrib.postgres.fields import ArrayField
from django.db import models
from django.utils.translation import gettext_lazy as _


class SomeEnum(models.TextChoices):
    ROCK = 'ROCK', _('ROCK')
    SCISSORS = 'SCISSORS', _('SCISSORS')
    PAPER = 'PAPER', _('PAPER')


class AdvancedDto(models.Model):
    json = models.JSONField(blank=True, null=True, verbose_name=_('Example: [{"foo": "bar"}]'))
    some_enum = models.CharField(blank=True, null=True, choices=SomeEnum.choices, max_length=8, verbose_name=_('Enum field with the same name as of different entity'))
    java_duration = models.CharField(max_length=32, blank=True, null=True)

    class Meta:
        abstract = True


class SomeEnum(models.TextChoices):
    VARIANT1 = 'variant1', _('variant1')
    VARIANT2 = 'variant2', _('variant2')
    VARIANT3 = 'variant3', _('variant3')


class BasicDto(models.Model):
    some_string = models.CharField(blank=True, null=True)
    some_integer = models.IntegerField(verbose_name=_('Field description'))
    some_number = models.FloatField(verbose_name=_('Field description'))
    some_boolean = models.BooleanField(blank=True, null=True)
    timestamp = models.DateTimeField(blank=True, null=True)
    some_enum = models.CharField(blank=True, null=True, choices=SomeEnum.choices, max_length=8)
    nested_object = models.JSONField(blank=True, null=True)
    combined_type = models.IntegerField(blank=True, null=True)

    class Meta:
        abstract = True


class ErrorResponse(models.Model):
    message = models.CharField(blank=True, null=True)
    proxied_error = models.JSONField(blank=True, null=True)

    class Meta:
        abstract = True


class Pageable(models.Model):
    page = models.IntegerField(blank=True, null=True)
    size = models.IntegerField(blank=True, null=True)
    sort = ArrayField(blank=True, null=True, base_field=models.CharField())

    class Meta:
        abstract = True

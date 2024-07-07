# Generated by DTO-Codegen

from django.db import models
from django.utils.translation import gettext_lazy as _


class EnumValue(models.TextChoices):
    VALUE_1 = 'value 1', _('name 1')
    VALUE_2 = 'value 2', _('name 2')
    VALUE_3 = 'value 3', _('name 3')


class BasicDTO(models.Model):
    timestamp = models.DateTimeField()
    optional_value = models.FloatField(default=0)
    nullable_value = models.BooleanField(blank=True, null=True)
    enum_value = models.CharField(choices=EnumValue.choices, max_length=7)
    documented_value = models.FloatField(verbose_name=_('short description'), help_text=_('very long description lol'))

    class Meta:
        abstract = True


class AdvancedDTO(models.Model):
    """
    entity with all-singing all-dancing properties
    """
    a = models.IntegerField()
    b = models.IntegerField()

    class Meta:
        abstract = True


GENERATED_MODELS = [
    AdvancedDTO,
    BasicDTO
]

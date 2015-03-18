# -*- coding: utf-8 -*-
from scrapy.item import Item, Field
# Define here the models for your scraped items
#
# See documentation in:
# http://doc.scrapy.org/en/latest/topics/items.html

import scrapy


class RessourcescrawlerItem(Item):
    # define the fields for your item here like:
    title = Field()
    body = Field()

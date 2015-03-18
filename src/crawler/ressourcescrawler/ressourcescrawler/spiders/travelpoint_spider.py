import scrapy
from scrapy.contrib.spiders import CrawlSpider, Rule
from scrapy.contrib.linkextractors import LinkExtractor
from ressourcescrawler.items import RessourcescrawlerItem

class TravelSpider(CrawlSpider):
    name = "travelpoint"
    start_urls = ["http://www.travellerspoint.com/guide/"]
    allowed_domains = ["www.travellerspoint.com"]


    rules = (
        # Extract links matching 'category.php' (but not matching 'subsection.php')
        # and follow links from them (since no callback means follow=True by default).


        # Extract links matching 'item.php' and parse them with the spider's method parse_item
        Rule(LinkExtractor(allow=('guide', )), callback='parse_item', follow= True),
    )


    def parse_item(self, response):
        #filename = response.url.split("/")[-2]
        #with open(filename, 'wb') as f:
        #    f.write(response.body)
        item = RessourcescrawlerItem()
        item['title'] = response.xpath('//title/text()').extract()
        item['body'] = response.xpath('//div[@id="wikitext"]').extract()
        return item

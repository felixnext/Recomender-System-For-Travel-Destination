#!/usr/bin/python

import sys
import json
import yaml
from bs4 import BeautifulSoup
import codecs

path = sys.argv.pop()

data = open(path, "r").read()
output = open(path + "_cleaned.xml", "w")
output.write("<data>\n")

json_data = json.dumps(data)

json_encoded = json.loads(json_data)

json_objects = yaml.load(json_encoded)

counter = 0

for json_object in json_objects:
    body = json_object['body']
    if len(body) < 1:
        continue

    counter = counter + 1

    title = json_object['title']
    title1 = title[0].replace("Travellers' Guide To ","")
    title1 = title1.replace(" - Wiki Travel Guide - Travellerspoint","")

    print title1
    output.write("  <place>" + title1.encode("utf-8") + "<\place>\n")

    soup = BeautifulSoup(body.pop())
    divs = soup.findAll('div',{'id':True})

    output.write("  <description>\n")
    for div in divs:
        div_id = div['id']
        output.write("    <paragraph name=\"" + div_id.encode("utf-8")+  "\">\n")
        content = soup.find_all("div", {"id": div_id})
        for tag in content:
            ps = tag.find_all('p')
            #get all text paragraph
            for p in ps:
                #todo ul
                text  = p.get_text().encode("utf-8")
                output.write("      <p>" + text + "<\p>\n")
            #get all cities in li tag
            lis = tag.find_all('li')
            for li in lis:
                #todo ul
                text  = li.get_text().encode("utf-8")
                output.write("      <li>" + text + "<\li>\n")

        output.write("    <\paragraph>\n")
    output.write("  <\descryption>\n")

output.write("<\data>\n")
output.close()

print str(counter) + "object extracted"

#!/usr/bin/python

from lxml import etree
import xml.etree.ElementTree as ET
import json
import re

#####################
#Creates json file from xml for loading data into elasticsearch
#####################

file_path = "/Users/yevgen/Documents/data/master/dumps/annotated/wikitravel_annotated.xml"
out_path = "/Users/yevgen/Documents/data/master/dumps/annotated/wikitravel_annotated.json"
index = "wikitravel"

file = open(file_path,"r")
out = open(out_path, "w")


dbpedia = False
text = False

dict = {}
paragraphs = []
paragraph = ""
paragraph_names = []

for line in file:
    if '<title>' in line:
        dict['title'] = line.replace('<title>', "").replace('</title>', "").strip()
    if '<lat>' in line:
        dict['lat'] = line.replace('<lat>',"").replace('</lat>',"").strip()
    if '<long>' in line:
        dict['long'] = line.replace('<long>',"").replace('</long>',"").strip()

    if '</dbpedia>' in line:
        dbpedia = False

    if '</text>' in line:
        text = False

        dict['paragraph_texts'] = paragraphs
        dict['paragraph_names'] = paragraph_names

        out.write("""{"create": { "_index": "%s", "_type": "traveldata" }}\n""" % index)
        out.write(json.dumps(dict) + "\n")

        dict = {}
        paragraphs = []
        paragraph_names = []

    if dbpedia:
        tmp_tag = ""
        tag = True
        tmp_value = ""
        val = 0
        value = False
        for c in line.strip():
            if c == ">":
                tag = False
                val = 1
            if tag:
                tmp_tag = tmp_tag + c
            if value == 1 and value == True and c == "<":
                value = False
                val = 0
            if value:
                tmp_value = tmp_value + c
            if val == 1:
                value = True
        tmp_tag = tmp_tag.replace("<","")

        #remove last markup
        if '^' in tmp_value:
            tmp_value= tmp_value.split("^").pop(0).replace("\"","")
        tmp_value = tmp_value.replace("_", " ")

        if tmp_value == "lat" and dict['lat'] == "":
            dict['lat'] = tmp_value
        elif tmp_value == "long" and dict['long'] == "":
                dict['long'] = tmp_value
        elif tmp_tag == "sameAs":
            if dict.has_key(tmp_tag):
                dict[tmp_tag].append(tmp_value)
            else:
                dict[tmp_tag] = [tmp_value]
        else:
            dict[tmp_tag] = tmp_value


    if text:
        if '<paragraph name' in line:
            pass
            #TODO get name & append name
        if '</paragraph>' in line:
            line = line.replace("</paragraph>","")
            paragraph = paragraph + re.escape(line)
            paragraphs.append(paragraph)
            paragraph = ""

        if '<paragraph name' not in line and '</paragraph>' not in line:
            pass

    if '<dbpedia>' in line:
        dbpedia = True

    if '<text>' in line:
        text = True

out.close()

#!/usr/bin/python

import json
import re
import io
import chardet
import sys
import os

#####################
#Creates json file from xml for loading data into elasticsearch
#####################




if len(sys.argv) != 3:
    raise Exception("Path and index were not specified in arguments! \nFORMAT: python CreateJsonBulk.py index pyth")


part = 0
file_name = sys.argv[2]
out_path = sys.argv[2].replace(".xml", "%s.json" % part)
index = sys.argv[1]

#r = chardet.detect(open(file_name).read())
#charenc = r['encoding']
#print "Encoding: " + charenc
file = io.open(file_name, "r",  encoding="utf-8")

out = open(out_path, "w")


dbpedia = False
text = False


def clean_text(text):
    if len(text.split(" ")) < 3:
        text = ""
    else:
        text = re.sub(r"(?:\@|https?\://)\S+", "", text)
        if index == "wikitravel":
            text = re.sub(re.compile('\W+'), " ", text)
    return text



dict = {}
paragraphs = []
paragraph = ""
paragraph_names = []

temperature = ["novMeanF", "julLowF", "julMeanF", "aprLowF", "marHighF", "janHighF", "febMeanF", "augLowF",
            "aprHighF", "deFHighF", "oFtLowF", "deFLowF", "junMeanF", "janLowF", "sepHighF", "mayLowF", "marMeanF",
            "sepMeanF", "julHighF", "janMeanF", "deFMeanF", "novHighF", "marLowF", "febLowF", "junHighF", "novLowF",
            "junLowF", "augMeanF", "mayHighF", "febHighF", "augHighF", "aprMeanF", "oFtHighF", "oFtMeanF", "sepLowF", "mayMeanF"]
doc_id = 0

for line in file:
    line = line.strip().encode("utf-8")

    if '<title>' in line:
        dict['title'] = line.replace('<title>', "").replace('</title>', "").strip()
    if '<lat>' in line and not dbpedia:
         lat = line.replace('<lat>',"").replace('</lat>',"").strip()
         if lat != "":
            dict['lat'] = lat
    if '<long>' in line  and not dbpedia:
        long = line.replace('<long>',"").replace('</long>',"").strip()
        if long != "":
            dict['long'] = long

    if '</dbpedia>' in line:
        dbpedia = False

    if '</text>' in line:
        text = False

        dict['paragraph_texts'] = paragraphs
        dict['paragraph_names'] = paragraph_names

        if dict.has_key('lat') and dict.has_key('long'):
            if dict['lat'] != "none" and dict['long'] != "none":
                dict['location'] = {'lat': dict['lat'], 'lon': dict['long']}
            else:
                print "None"
            del dict['lat']
            del dict['long']

        size = float(os.path.getsize(out_path))/1000000.0
        #plit file into smaller chnks
        #if file is greater than 1400mb
        if (1399.0 - size) < 0.0001:
            out.close()
            part = part + 1
            out_path = sys.argv[2].replace(".xml", "%s.json" % part)
            out = open(out_path, "w")

        out.write("""{"create": { "_index": "%s", "_type": "traveldata", "_id" : "%s" }}\n""" % (index, doc_id))
        out.write(json.dumps(dict).encode('utf-8') + "\n")

        doc_id = doc_id + 1

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

        if any(tmp_tag in s for s in temperature):
            tmp_tag = tmp_tag.replace("F","C")

        if "lat" == tmp_tag and not dict.has_key("lat"):
            dict['lat'] = tmp_value
            #print dict['lat']
        elif "long" == tmp_tag and not dict.has_key("long"):
                dict['long'] = tmp_value
        elif tmp_tag == "sameAs":
            if dict.has_key(tmp_tag):
                dict[tmp_tag].append(tmp_value)
            else:
                dict[tmp_tag] = [tmp_value]
        elif tmp_tag == "country":
            tmp_value = tmp_value.replace("\"","").split("@")[0]
            if dict.has_key(tmp_tag):
                dict[tmp_tag].append(tmp_value)
            else:
                dict[tmp_tag] = [tmp_value]
        else:
            dict[tmp_tag] = tmp_value


    if text:

        #print "ORIGINAL\n" + line + "\n\n\n\n"
        switcher = True

        if '<paragraph name' in line and switcher:
            line = line.replace("<paragraph name=", "")


            save = False
            if '</paragraph>' in line:
                save = True
                line = line.replace("</paragraph>","")

            is_title = 0
            title = ""
            is_text = False
            text = ""
            for c in line:
                if c == "\"":
                    is_title += 1

                if is_title == 1:
                    title = title + c

                if is_text and is_title > 1:
                    text = text + c

                if c == ">":
                    is_text = True


            title = title.replace("\"","")



            paragraph_names.append(title)
            paragraph = paragraph + clean_text(text)

            if save:
                paragraphs.append(paragraph)
                paragraph = ""

            switcher = False

        if '</paragraph>' in line and '<paragraph name' not in line and switcher:

            line = line.replace("</paragraph>","")
            paragraph = paragraph + clean_text(line)
            paragraphs.append(paragraph)

            paragraph = ""
            switcher = False

        if '<paragraph name' not in line and '</paragraph>' not in line and switcher:
            paragraph = paragraph + clean_text(line)
            paragraphs.append(paragraph)

            switcher = False

    if '<dbpedia>' in line:
        dbpedia = True

    if '<text>' in line:
        text = True

out.close()

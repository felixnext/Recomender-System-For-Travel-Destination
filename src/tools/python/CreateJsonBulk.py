#!/usr/bin/python

import json
import re
import io
import chardet

#####################
#Creates json file from xml for loading data into elasticsearch
#####################


def clean_text(text):
    if len(text.split(" ")) < 3:
        text = ""
    else:
        text = re.sub(r"(?:\@|https?\://)\S+", "", text)
        text = re.sub(re.compile('\W+'), " ", text)
    return text

file_name = "/Users/yevgen/Documents/data/master/dumps/annotated/wikitravel_annotated.xml"
out_path = "/Users/yevgen/Documents/data/master/dumps/annotated/wikitravel_annotated.json"
index = "wikitravel"

#r = chardet.detect(open(file_name).read())
#charenc = r['encoding']
#print "Encoding: " + charenc
file = io.open(file_name, "r",  encoding="utf-8")

out = open(out_path, "w")


dbpedia = False
text = False

dict = {}
paragraphs = []
paragraph = ""
paragraph_names = []

for line in file:
    line = line.strip().encode("utf-8")

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
        out.write(json.dumps(dict).encode('utf-8') + "\n")

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

            print title + "\n\n"
            print paragraph + "\n\n\n\n\n"

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

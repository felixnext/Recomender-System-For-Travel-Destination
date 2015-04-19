#!/usr/bin/python

import re

file = "/Users/yevgen/Desktop/classes"

in_ = open(file, "r")
out = open(file + ".json", "w")

index = 0

for line in in_:
    split = line.strip().split(',')
    uri = split[1].replace("dbpedia:", "http://dbpedia.org/ontology/")
    tokens = re.findall(r'\"(.+?)\"', split[0])
    words =" ".join(tokens)

    out.write("""{"create": { "_index": "dbpedia_classes", "_type": "class_mapping", "_id" : "%s" }}\n""" % index)
    out.write("""{"uri": "%s", "text": "%s"}\n""" % (uri, words))
    index = index + 1


out.close()

#!/usr/bin/python

##################
# Creates json bulk from patty file for indexing with elasticsearch
##################


file = "/Users/yevgen/Downloads/patty-dataset/dbpedia-relation-paraphrases.txt"

in_ = open(file, "r")
out = open(file.replace(".txt", ".json"), "w")

index = 0

for line in in_:
    split =line.split("\t")
    dbpedia = split[0]
    relation = split[1].strip()[:-1]

    out.write("""{"create": { "_index": "patty", "_type": "relation_mapping", "_id" : "%s" }}\n""" % index)
    out.write("""{"dbpedia_relation": "%s", "text_relation": "%s"}\n""" % (dbpedia, relation))
    index = index + 1


out.close()

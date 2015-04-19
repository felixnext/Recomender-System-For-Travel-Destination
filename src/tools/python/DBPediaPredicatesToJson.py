#!/usr/bin/python

import json
from pprint import pprint
from collections import namedtuple
import re

file = "/Users/yevgen/Downloads/sparql"
file_out = "/Users/yevgen/Downloads/DbpediaPred.json"

json_data = open(file, "r").read()
out = open(file_out, "w")

data = json.loads(json_data)
#pprint(data["results"]["bindings"])

Pred = namedtuple("Pred", "type, value")

preds = [Pred(**k["pred"]) for k in data["results"]["bindings"]]

index = 0

for p in preds:
    uri = p.value
    tokens = uri.split('/')
    whitespaced = re.sub(r'([A-Z])', r' \1', tokens[len(tokens) - 1]).lower()

    #print whitespaced

    out.write("""{"create": { "_index": "dbpedia_pred", "_type": "props_mapping", "_id" : "%s" }}\n""" % index)
    out.write("""{"dbpedia_uri": "%s", "text_relation": "%s"}\n""" % (uri, whitespaced))

    index = index + 1

#!/usr/bin/python

import sys
import json
import yaml

path = sys.argv.pop()

data = open(path, "r").read()

json_data = json.dumps(data)

json_encoded = json.loads(json_data)

json_objects = yaml.load(json_encoded)


for json_object in json_objects:
    json_object['body']
    print json_object['title']

#for key in json_data:
#    print key

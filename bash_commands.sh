#!/usr/bin/env bash

######################
#Annotate raw dumps
######################

#starts travelerpoint dump dbpedia annotation
nohup java -jar -Xmx5G Destination-Recomender-System-assembly-0.1.jar trevelerpoint ./items.json_cleaned.xml &

#starts wikipedia dump dbpedia annotation
nohup java -jar -Xmx5G Destination-Recomender-System-assembly-0.1.jar wikipedia ./wikipedia_articles.xml &

#starts travelerwiki dump annotation
nohup java -jar -Xmx5G Destination-Recomender-System-assembly-0.1.jar trevelerswiki ./wikitravelorg_wiki_en-20110605-current.xml &

######################
#elasticsearch
######################

#transform data into  elastic search bulk format
python CreateJsonBulk.py travellerspoint travellerspoint.xml

#start elasticsearch node
bin/elasticsearch -d -Xmx5g -Xms5g -Des.index.store.type=memory --node.name=RS1

#indicies: wikipedia, travellerspoint, wikitravel
#create a index with specific settings: english analyzer
curl -XPUT 'localhost:9200/wikipedia' -d '{
  "settings": {
    "analysis": {
      "filter": {
        "english_stop": {
          "type":       "stop",
          "stopwords":  "_english_"
        },
        "english_stemmer": {
          "type":       "stemmer",
          "language":   "english"
        },
        "english_possessive_stemmer": {
          "type":       "stemmer",
          "language":   "possessive_english"
        }
      },
      "char_filter": {
        "&_to_and": {
            "type": "mapping",
            "mappings": [ "& => and"]
        }
      },
      "analyzer": {
        "english": {
          "tokenizer":  "standard",
          "char_filter": [
            "html_strip",
             "&_to_and"
          ],
          "filter": [
            "english_possessive_stemmer",
            "lowercase",
            "english_stop",
            "english_stemmer"
          ]
        }
      }
    }
  }
}'

#bulk load data
curl -XPOST 'localhost:9200/_bulk?pretty' --data-binary @my_file.json


#get status
curl 'localhost:9200/_cat/indices?v'

#request mapping: how data was interpreted?
curl -XGET 'localhost:9200/gb/_mapping/tweet'

#delete index
curl -XDELETE 'http://localhost:9200/index/'

#stop elasticsearch
curl -XPOST 'http://localhost:9200/_shutdown'

#validate a query
curl -XGET 'localhost:9200/gb/tweet/_validate/query?explain'

#!/usr/bin/env bash

######################
#Annotate raw dumps
######################

#starts travelerpoint dump dbpedia annotation
nohup java -jar -Xmx5G Destination-Recomender-System-assembly-0.1.jar trevelerpoint ./items.json_cleaned.xml &

#starts wikipedia dump dbpedia annotation
nohup java -jar -Xmx30G Destination-Recomender-System-assembly-0.1.jar wikipedia ./wikipedia_articles.xml &

#starts travelerwiki dump annotation
nohup java -jar -Xmx30G Destination-Recomender-System-assembly-0.1.jar trevelerswiki ./wikitravelorg_wiki_en-20110605-current.xml &

######################
#elasticsearch
######################

#start elasticsearch node
bin/elasticsearch -d -Xmx5g -Xms5g -Des.index.store.type=memory --node.name=RS1

#bulk load data
curl -XPOST '134.169.32.163:9200/_bulk?pretty' --data-binary @my_file.json

#indicies: wikipedia, travellerspoint, wikitravel
#get status
curl '134.169.32.163:9200/_cat/indices?v'

#request mapping: how data was interpreted?
curl -XGET 'localhost:9200/gb/_mapping/tweet'

#delete index
curl -XDELETE 'http://localhost:9200/index/'

#stop elasticsearch
curl -XPOST 'http://localhost:9200/_shutdown'
#!/usr/bin/env bash

#starts travelerpoint dump dbpedia annotation
nohup java -jar -Xmx30G Destination-Recomender-System-assembly-0.1.jar trevelerpoint ./items.json_cleaned.xml &

#starts wikipedia dump dbpedia annotation
nohup java -jar -Xmx30G Destination-Recomender-System-assembly-0.1.jar wikipedia ./wikipedia_articles.xml &

#starts travelerwiki dump annotation
nohup java -jar -Xmx30G Destination-Recomender-System-assembly-0.1.jar trevelerswiki ./wikitravelorg_wiki_en-20110605-current.xml &

######################
#elasticsearch
#######################

#start elasticsearch
bin/elasticsearch -d -Xmx5g -Xms5g -Des.index.store.type=memory --Des.max-open-files=64000 --node.name=RS1

#bulk load data
curl -XPOST 'localhost:9200/_bulk?pretty' --data-binary @file.json

#stop elasticsearch
curl -XPOST 'http://localhost:9200/_shutdown'
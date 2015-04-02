#!/usr/bin/env bash

#starts travelerpoint dump dbpedia annotation
nohup java -jar -Xmx30G Destination-Recomender-System-assembly-0.1.jar trevelerpoint ./items.json_cleaned.xml &

#starts wikipedia dump dbpedia annotation
nohup java -jar -Xmx30G Destination-Recomender-System-assembly-0.1.jar wikipedia ./wikipedia_articles.xml &

#starts travelerwiki dump annotation
nohup java -jar -Xmx30G Destination-Recomender-System-assembly-0.1.jar trevelerswiki ./wikitravelorg_wiki_en-20110605-current.xml &
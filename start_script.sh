#!/usr/bin/env bash

#starts travelerpoint dump dbpedia annotation
nohup java -jar -Xmx100G Destination-Recomender-System-assembly-0.1.jar trevelerpoint ./items.json_cleaned.xml &

#starts wikipedia dump dbpedia annotation
nohup java -jar -Xmx100G Destination-Recomender-System-assembly-0.1.jar wikipedia ./wikipedia_articles.xml &


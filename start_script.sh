#!/usr/bin/env bash
nohup java -jar -Xmx100G Destination-Recomender-System-assembly-0.1.jar trevelerpoint ./items.json_cleaned.xml &

nohup java -jar -Xmx100G Destination-Recomender-System-assembly-0.1.jar wikipedia ./wikipedia_articles.xml &
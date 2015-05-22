# Recommender System For Travel Destinations

Motivation
----------

How to find your best travel destination? There is countless number of online travel agents, booking engines,
review sites and with enormous search time and some cups of coffee you will find possible vocations.
This service is helping to find travel destinations based on your preferences just by describing your perfect vocation in natural language.


REST api
--------
    
    $ curl -XPOST 'http://localhost:8080/search' -H "Content-Type: application/json" \
     -d  '{"query": "My destination description...."}'
     
The response contains a sorted list of locations with corresponding name, coordinates and score.

Build
-----

    $ sbt assembly
    
Technology
----------

* [Akka](http://akka.io) was used for parallel relation extraction
* [Spray](http://spray.io) provides http rest service
* [MLlib](https://spark.apache.org/mllib/) for the re-ranking of combined results
* [Spark](https://spark.apache.org/) for model computation and parallel tf-idf calculation of relations
* [CoreNLP] (http://nlp.stanford.edu/software/corenlp.shtml) for text annotation
* [Elasticsearch] (https://www.elastic.co) for search on extracted relation and full-text search on corpora
* [Clavin] (https://clavin.bericotechnologies.com) for document geotagging and geoparsing
* [Virtuoso] (https://www.w3.org/2001/sw/wiki/OpenLink_Virtuoso) as Triple Store for DBPedia data
* [DBpedia Spotlight] (https://github.com/dbpedia-spotlight/dbpedia-spotlight) for dbpedia entity extraction
* [OpenIE](https://github.com/knowitall/openie) for relation extraction

Data
-----
Json dump for elasticsearch bulk loading can be downloaded [here](https://www.dropbox.com/s/0jmj6dtnfir4bpo/elastic.tar.bz2?dl=0).
Dataset contains descriptions, names and metadata (e.g. lat, long) of locations.


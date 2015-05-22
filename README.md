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

Data
-----
Json dump for elasticsearch bulk loading can be downloaded [here](https://www.dropbox.com/s/0jmj6dtnfir4bpo/elastic.tar.bz2?dl=0).
Dataset contains descriptions, names and metadata (e.g. lat, long) of locations.


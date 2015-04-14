# Recomender-System-For-Travel-Destination

REST api
--------
    
    $ curl -XPOST 'http://localhost:8080/search' -H "Content-Type: application/json" 
    4 -d  '{"query": "My destination description."}'

Build
-----

    $ sbt assembly

Data
-----
Json dump for elasticsearch bulk loading can be downloaded [here](https://www.dropbox.com/s/0jmj6dtnfir4bpo/elastic.tar.bz2?dl=0).
Dataset contains descriptions, names and metadata (e.g. lat, long) of locations.

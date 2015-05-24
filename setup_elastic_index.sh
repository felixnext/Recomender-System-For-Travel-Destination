#!/usr/bin/env bash

#transform data into  elastic search bulk format
#python CreateJsonBulk.py travellerspoint travellerspoint.xml

#start elasticsearch node
#bin/elasticsearch -d -Xmx5g -Xms5g -Des.index.store.type=memory --node.name=RS1

#indicies: wikipedia, travellerspoint, wikitravel

#ASSUMPTION: elastic claster was configured and all nodes were started

#delete existing indexes
curl -XDELETE 'http://localhost:9200/wikitravel/'
curl -XDELETE 'http://localhost:9200/patty/'
curl -XDELETE 'http://localhost:9200/dbpedia_pred/'
curl -XDELETE 'http://localhost:9200/dbpedia_classes/'
curl -XDELETE 'http://localhost:9200/structuredrelations/'

#TODO improve mappings

#create a index with specific settings: english analyzer
curl -XPUT 'localhost:9200/wikitravel' -d '
{
  "settings":{
    "index":{
      "analysis":{
        "filter":{
          "english_stop":{
            "type":"stop",
            "stopwords":"_english_"
          },
          "english_stemmer":{
            "type":"stemmer",
            "language":"english"
          },
          "english_possessive_stemmer":{
            "type":"stemmer",
            "language":"possessive_english"
          }
        },
        "char_filter":{
          "&_to_and":{
            "type":"mapping",
            "mappings":[
              "& => and"
            ]
          }
        },
        "analyzer":{
          "my_english":{
            "tokenizer":"standard",
            "char_filter":[
              "html_strip",
              "&_to_and"
            ],
            "filter":[
              "english_possessive_stemmer",
              "lowercase",
              "asciifolding",
              "english_stop",
              "english_stemmer"
            ]
          }
        }
      }
    }
  },
  "mappings":{
    "traveldata":{
      "dynamic_templates":[
        {
          "en":{
            "match":"*",
            "match_mapping_type":"string",
            "mapping":{
              "type":"string",
              "analyzer":"my_english"
            }
          }
        }
      ],
      "properties":{
        "location":{
          "type":"geo_point"
        },
        "title":{
          "type":"string",
          "index":"not_analyzed"
        },
        "novMeanC":{
          "type":"double"
        },
        "julLowC":{
          "type":"double"
        },
        "julMeanC":{
          "type":"double"
        },
        "aprLowC":{
          "type":"double"
        },
        "marHighC":{
          "type":"double"
        },
        "janHighC":{
          "type":"double"
        },
        "febMeanC":{
          "type":"double"
        },
        "augLowC":{
          "type":"double"
        },
        "aprHighC":{
          "type":"double"
        },
        "decHighC":{
          "type":"double"
        },
        "octLowC":{
          "type":"double"
        },
        "decLowC":{
          "type":"double"
        },
        "junMeanC":{
          "type":"double"
        },
        "janLowC":{
          "type":"double"
        },
        "sepHighC":{
          "type":"double"
        },
        "mayLowC":{
          "type":"double"
        },
        "marMeanC":{
          "type":"double"
        },
        "sepMeanC":{
          "type":"double"
        },
        "julHighC":{
          "type":"double"
        },
        "janMeanC":{
          "type":"double"
        },
        "decMeanC":{
          "type":"double"
        },
        "novHighC":{
          "type":"double"
        },
        "marLowC":{
          "type":"double"
        },
        "febLowC":{
          "type":"double"
        },
        "junHighC":{
          "type":"double"
        },
        "novLowC":{
          "type":"double"
        },
        "junLowC":{
          "type":"double"
        },
        "augMeanC":{
          "type":"double"
        },
        "mayHighC":{
          "type":"double"
        },
        "febHighC":{
          "type":"double"
        },
        "augHighC":{
          "type":"double"
        },
        "aprMeanC":{
          "type":"double"
        },
        "octHighC":{
          "type":"double"
        },
        "octMeanC":{
          "type":"double"
        },
        "sepLowC":{
          "type":"double"
        },
        "mayMeanC":{
          "type":"double"
        },
        "novSun":{
          "type":"double"
        },
        "sepSun":{
          "type":"double"
        },
        "octSun":{
          "type":"double"
        },
        "janSun":{
          "type":"double"
        },
        "junSun":{
          "type":"double"
        },
        "augSun":{
          "type":"double"
        },
        "marSun":{
          "type":"double"
        },
        "decSun":{
          "type":"double"
        },
        "maySun":{
          "type":"double"
        },
        "aprSun":{
          "type":"double"
        },
        "febSun":{
          "type":"double"
        },
        "julSun":{
          "type":"double"
        },
        "paragraph_names": {
            "type": "string",
            "index":"not_analyzed"
        },
        "sameAs": {
            "type": "string",
            "index":"not_analyzed"
        },
        "country": {
            "type": "string",
            "index":"not_analyzed"
        }
        "paragraph_texts": {
            "type": "string",
            "position_offset_gap": 100,
            "analyzer": "my_english"
        }
      }
    }
  }
}'

curl -XPUT 'localhost:9200/patty' -d '
{
    "mappings":{
      "relation_mapping":{
        "properties":{
          "dbpedia_relation":{
            "type":"string",
            "index":"not_analyzed"
          },
          "text_relation":{
            "type":"string",
            "analyzer":"whitespace"
          }
        }
      }
    }
}'

curl -XPUT 'localhost:9200/dbpedia_pred' -d '
{
    "mappings":{
      "props_mapping":{
        "properties":{
          "dbpedia_uri":{
            "type":"string",
            "index":"not_analyzed"
          },
          "text_relation":{
            "type":"string",
            "analyzer":"whitespace"
          }
        }
      }
    }
}'

curl -XPUT 'localhost:9200/dbpedia_classes' -d '
{
    "mappings":{
      "class_mapping":{
        "properties":{
          "uri":{
            "type":"string",
            "index":"not_analyzed"
          },
          "text":{
            "type":"string",
            "analyzer":"whitespace"
          }
        }
      }
    }
}'

curl -XPUT 'localhost:9200/structuredrelations' -d '
{
  "settings":{
    "index":{
      "analysis":{
        "filter":{
          "english_stop":{
            "type":"stop",
            "stopwords":"_english_"
          },
          "english_stemmer":{
            "type":"stemmer",
            "language":"english"
          },
          "english_possessive_stemmer":{
            "type":"stemmer",
            "language":"possessive_english"
          }
        },
        "char_filter":{
          "&_to_and":{
            "type":"mapping",
            "mappings":[
              "& => and"
            ]
          }
        },
        "analyzer":{
          "my_english":{
            "tokenizer":"standard",
            "char_filter":[
              "html_strip",
              "&_to_and"
            ],
            "filter":[
              "english_possessive_stemmer",
              "lowercase",
              "asciifolding",
              "english_stop",
              "english_stemmer"
            ]
          }
        }
      }
    }
  },
  "mappings":{
    "relations":{
      "dynamic_templates":[
        {
          "en":{
            "match":"*",
            "match_mapping_type":"string",
            "mapping":{
              "type":"string",
              "analyzer":"my_english"
            }
          }
        }
      ],
      "properties":{
        "locationName":{
          "type":"string",
          "analyzer":"simple"
        },
        "rel": {
            "type": "string",
            "analyzer": "my_english"
        },
        "objCand": {
            "type": "string",
            "analyzer": "my_english"
        },
        "subjCand": {
            "type": "string",
            "analyzer": "my_english"
        },
        "id":{
          "type":"integer",
          "index":"not_analyzed"
        },
        "sent":{
          "type":"string",
          "index":"not_analyzed"
        },
        "tfIdf":{
          "type":"double",
          "index":"not_analyzed"
        }
      }
    }
  }
}'

curl -XPOST 'localhost:9200/_bulk?wikitravel' --data-binary @file0.json > wikitravel_load0.log
curl -XPOST 'localhost:9200/_bulk?wikitravel' --data-binary @file1.json > wikitravel_load1.log
curl -XPOST 'localhost:9200/_bulk?wikitravel' --data-binary @file2.json > wikitravel_load2.log
curl -XPOST 'localhost:9200/_bulk?wikitravel' --data-binary @file3.json > wikitravel_load3.log
curl -XPOST 'localhost:9200/_bulk?patty' --data-binary @dbpedia-relation-paraphrases.json > patty_load.log
curl -XPOST 'localhost:9200/_bulk?dbpedia_pred' --data-binary @DbpediaPred.json > dbpedia_props.log
curl -XPOST 'localhost:9200/_bulk?dbpedia_classes' --data-binary @classes.json > classes.json.log
curl -XPOST 'localhost:9200/_bulk?structuredrelations' --data-binary @relations_idf.json > relations.log

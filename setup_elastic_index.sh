#!/usr/bin/env bash

#transform data into  elastic search bulk format
#python CreateJsonBulk.py travellerspoint travellerspoint.xml

#start elasticsearch node
#bin/elasticsearch -d -Xmx5g -Xms5g -Des.index.store.type=memory --node.name=RS1

#indicies: wikipedia, travellerspoint, wikitravel

#delete index
curl -XDELETE 'http://localhost:9200/wikipedia/'
curl -XDELETE 'http://localhost:9200/travellerspoint/'
curl -XDELETE 'http://localhost:9200/wikitravel/'

#create a index with specific settings: english analyzer
curl -XPUT 'localhost:9200/wikipedia' -d '
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
    "wikidump":{
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
        },
        "paragraph_texts": {
            "type": "string",
            "position_offset_gap": 100,
            "analyzer": "my_english"
        }
      }
    }
  }
}'

curl -XPUT 'localhost:9200/travellerspoint' -d '
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
    "wikidump":{
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
        },
        "paragraph_texts": {
            "type": "string",
            "position_offset_gap": 100,
            "analyzer": "my_english"
        }
      }
    }
  }
}'

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
    "wikidump":{
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
        },
        "paragraph_texts": {
            "type": "string",
            "position_offset_gap": 100,
            "analyzer": "my_english"
        }
      }
    }
  }
}'

curl -XPOST 'localhost:9200/_bulk?pretty' --data-binary @travellerspoint.json > travellerspoint_load.log
curl -XPOST 'localhost:9200/_bulk?pretty' --data-binary @wikipedia.json > wikipedia_load.log
curl -XPOST 'localhost:9200/_bulk?pretty' --data-binary @wikitravel.json > wikitravel_load.log

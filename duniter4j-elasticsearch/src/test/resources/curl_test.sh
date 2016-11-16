#!/bin/sh

curl -XPOST "http://data.duniter.fr/market/record/_search?pretty&scroll=1m" -d'
{
  "query": {
        
        "bool":{
            "should": {
                "range":{
                    "time":{
                        "gte":0
                    }
                }
            },
            "filter": [
                {"term":{
                        "currency":"sou"
                    }
                }                
            ]
        }
  },
  "from":0,
  "size":100
}'


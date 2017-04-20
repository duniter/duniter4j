#!/bin/sh

curl -XPOST 'http://localhost:9200/g1/block/_search?pretty' -d '
   {
     "size": 10000,
     "query": {
         "filtered": {
           "filter": {
             "bool": {
               "must": [
                 {
                   "exists": {
                     "field": "dividend"
                   }
                 }
               ]
             }
           }
         }
       },
       "_source": ["dividend", "monetaryMass", "membersCount"],
       sort
   }'

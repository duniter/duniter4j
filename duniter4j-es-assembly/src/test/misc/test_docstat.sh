#!/bin/sh

curl -XPOST 'http://localhost:9200/docstat/record' -d '
   {
     "index":"user",
     "type":"profile",
     "count":874,
     "time":1505297350
   }'


curl -XPOST 'http://localhost:9200/docstat/record/_search?pretty' -d '
   {
     "size": 0
   }'

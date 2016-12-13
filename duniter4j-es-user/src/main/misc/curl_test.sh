#!/bin/sh

curl -XPOST "http://127.0.0.1:9200/user/event/_count?pretty" -d'
{
  query: {
    bool: {
        filter: [
          {term: {recipient: "5ocqzyDMMWf1V8bsoNhWb1iNwax1e9M7VTUN6navs8of"}}
        ],
        must_not: {terms: { "code": ["TX_SENT"]}}
    }
  },
  sort : [
    { "time" : {"order" : "desc"}}
  ],
  from: 0,
  size: 100,
  _source: false
}'


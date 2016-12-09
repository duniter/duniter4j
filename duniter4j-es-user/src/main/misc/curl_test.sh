#!/bin/sh

curl -XPOST "http://127.0.0.1:9200/user/event/_search?pretty" -d'
{
  query: {
    nested: {
        path: "reference",
        query: {
            constant_score: {
              filter:
                  [
                      {term: { "reference.index": "test_net"}},
                      {term: { "reference.type": "block"}},
                      {term: { "reference.id": "10862"}}
                  ]

          }
        }
    }

  },
  from: 0,
  size: 100
}'


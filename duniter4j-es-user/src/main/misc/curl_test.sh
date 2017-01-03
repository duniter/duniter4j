#!/bin/sh

echo "--- COUNT query --- "
curl -XPOST "http://127.0.0.1:9200/user/event/_count?pretty" -d'
{
  query: {
    bool: {
        must: [
          {term: {recipient: "5ocqzyDMMWf1V8bsoNhWb1iNwax1e9M7VTUN6navs8of"}},
          {range: {time: {gt: 0}}}
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

echo "--- IDS query --- "
curl -XPOST "http://127.0.0.1:9200/market/comment/_search?pretty" -d'
{
  query: {
    terms: {
      _id: [ "AVlA2v8sW3j-KIPA7pm8" ]
    }
  },
  from: 0,
  size: 100
}'

echo "--- COUNT query --- "
curl -XPOST "http://127.0.0.1:9200/market/comment/_search?pretty" -d'
{
  query: {
    terms: {
        reply_to: ["AVlEmFhF1r62y3TgqdyR"]
    }
  },
  "aggs" : {
      "reply_tos" : {
          "terms" : { "field" : "reply_to" }
      }
  },
  size: 0
}'

echo "--- COUNT + GET query --- "
curl -XPOST "http://127.0.0.1:9200/market/comment/_search?pretty" -d'
{
  query: {
      terms: {
        record: [ "AVk_pr_49ItF-SEayNy1" ]
      }
  },
  "aggs" : {
      "reply_tos" : {
          "terms" : { "field" : "reply_to" }
      }
  },
  sort : [
    { "time" : {"order" : "desc"}}
  ],
  from: 0,
  size: 5
}'

echo "--- GET user event --- "
curl -XPOST "http://127.0.0.1:9200/user/event/_search?pretty" -d'
{
  query: {
    bool: {
        filter: [
          {term: { recipient: "5ocqzyDMMWf1V8bsoNhWb1iNwax1e9M7VTUN6navs8of"}},
          {term: { code: "TX_RECEIVED"}}
        ]
      }
  },
  sort : [
    { "time" : {"order" : "desc"}}
  ],
  from: 0,
  size: 3
}'



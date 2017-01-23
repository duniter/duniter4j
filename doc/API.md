
# HTTP API

## Contents

* [Contents](#contents)
* [Overview](#overview)
* [ES CORE API](#es-core-api)
  * [currency](#currency)
      * [currency/block](#currencyblock)
* [ES USER API](#userapi)
  * [user](#user)
      * [user/profile](#userprofile)
      * [user/settings](#usersettings)
  * [message](#message)
      * [message/inbox](#messageinbox)
      * [message/oubox](#messageoutbox)
* [ES GCHANGE API](#gchangeapi)
  * [market](#market)
      * [market/category](#marketcategory)
      * [market/record](#marketrecord)
      * [market/comment](#marletcomment)
  * [registry](#registry)
      * [registrymarket/category](#registrycategory)
      * [registry/record](#registryrecord)
      * [registry/comment](#registrycomment)

## Overview

Duniter4j Elasticsearch offer HTTP access to 3 main API :

 - `ES CORE API` (ECA): BlockChain indexation;
 - `ES USER API` (EUA): User data indexation, such as: profiles, private messages, settings (crypted);
 - `ES GCHANGE API` (EGA): Exchange data (market place: offer, ad...), professionals registry. 


Data is made accessible through an HTTP API :

    http[s]://node[:port]/...
    |-- <currency_name>/
    |   `-- lookup
    |-- user/
    |   |-- profile
    |   `-- settings
    |-- message/
    |   |-- inbox
    |   `-- outbox
    |-- market/
    |   |-- category
    |   |-- record
    |   `-- comment
    `-- registry/
        |-- category
        |-- record
        `-- comment

### Document format
 
All stored documents use a JSON format.

Every document must have the following fields:

- `issuer` : The document's emitter
- `hash`:
- `signature`: the signature emitted by the issuer.


## ES CORE API

### `<currency>/*`

#### `<currency>/block`

## ES USER API

### `user/*`

#### `user/profile`

#### `user/settings`

### `message/*`

#### `message/inbox`

#### `message/outbox`

## ES GCHANGE API

### `market/*`

#### `market/category`

#### `market/record`

#### `market/comment`

### `registry/*`

#### `registry/category`

#### `registry/record`

#### `registry/comment`

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
  * [invitation](#invitation)
      * [invitation/certification](#invitationcertification)
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
    |-- invitation/
    |   `-- certification
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

#### Data document

Every document have the following mandatory fields:

- `issuer` : The document's emitter
- `hash`:
- `signature`: the signature emitted by the issuer.

#### Deletion

Document deletion use a document with this mandatory fields:

- `index` : The document's index
- `type` : The document's type
- `issuer`: The deletion issuer. Should correspond to the document's `issuer`, or the `recipient` in some special case ([inbox message](#messageinbox) or [invitation](#invitation))
- `time`: the current time
- `hash`
- `signature`.

For example, a deletion on `message/inbox` should send this document:

```json

```
          
## ES CORE API

### `<currency>/*`

#### `<currency>/block`

 - Get the current block: `<currency>/block/current`
 - Get a block by number: `<currency>/block/<number>`
 - Search on blocks: `<currency>/block/_search` (POST or GET)

## ES USER API

### `user/*`

#### `user/profile`

#### `user/settings`

### `message/*`

#### `message/inbox`

#### `message/outbox`

### `invitation/*`

#### `invitation/certification`

 - Get an invitation, by id: `invitation/certification/<id>`
 - Add a new invitation: `invitation/certification` (POST)
 - Delete an existing invitation: `invitation/certification/_delete` (POST)
 - Search on invitations: `invitation/certification/_search` (POST or GET)


## ES GCHANGE API

### `market/*`

#### `market/category`

#### `market/record`

#### `market/comment`

### `registry/*`

#### `registry/category`

#### `registry/record`

#### `registry/comment`
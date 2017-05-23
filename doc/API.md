
# HTTP API

## Contents

* [Contents](#contents)
* [Overview](#overview)
* [ES CORE API](#es-core-api)
  * [currency](#currency)
      * [currency/block](#currencyblock)
      * [currency/blockstat](#currencyblockstat)
      * [currency/peer](#currencypeer)
      * [currency/tx](#currencytx)
* [ES USER API](#userapi)
  * [user](#user)
      * [user/event](#userevent)
      * [user/profile](#userprofile)
      * [user/settings](#usersettings)
  * [message](#message)
      * [message/inbox](#messageinbox)
      * [message/oubox](#messageoutbox)
  * [invitation](#invitation)
      * [invitation/certification](#invitationcertification)

## Overview

Duniter4j Elasticsearch offer HTTP access to 3 main API :

 - `ES CORE API` (ECA): BlockChain indexation;
 - `ES USER API` (EUA): User data indexation, such as: profiles, private messages, settings (crypted);

Data is made accessible through an HTTP API :

    http[s]://node[:port]/...
    |-- <currency_name>/
    |   |-- block
    |   |-- blockstat
    |   |-- peer
    |   `-- tx
    |-- user/
    |   |-- profile
    |   `-- settings
    |-- message/
    |   |-- inbox
    |   `-- outbox
    `-- invitation/
        `-- certification

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

#### `<currency>/blockstat`

#### `<currency>/peer`


## ES USER API

### `user/*`

#### `user/event`

 - Get events on an account, by pubkey: `user/event/_search?q=issuer:<pubkey>` (GET)
 - Search on events: `user/event/_search` (POST or GET)

#### `user/profile`


 - Get an profile, by public key: `user/profile/<pubkey>`
 - Add a new profile: `user/profile` (POST)
 - Update an existing profile: `user/profile/_update` (POST)
 - Delete an existing invitation: `invitation/certification/_delete` (POST)
 - Search on profiles: `user/profile/_search` (POST or GET)

A profile document is a JSON document. Mandatory fields are:
 
 - `title`: user name (Lastanem, firstname...)
 - `time`: submission time, in seconds
 - `issuer`: user public key
 - `hash`: hash of the JSON document (without fields `hash` and `signature`)
 - `signature`: signature of the JSON document (without fields `hash` and `signature`)

Example with only mandatory fields:

```json
{
    "title" : "Pecquot Ludovic",
    "description" : "Développeur Java et techno client-serveur\nParticipation aux #RML7, #EIS et #Sou",
    "time" : 1488359903,
    "issuer" : "2v6tXNxGC1BWaJtUFyPJ1wJ8rbz9v1ZVU1E1LEV2v4ss",
    "hash" : "F66D43ECD4D38785F424ADB68B3EA13DD56DABDE275BBE780E81E8D4E1D0C5FA",
    "signature" : "3CWxdLtyY8dky97RZBFLfP6axnfW8KUmhlkiaXC7BN98yg6xE9CkijRBGmuyrx3llPx5HeoGLG99DyvVIKZuCg=="
}
```

Some additional fields are `description`, `socials`, `tags` and `avatar` :

```json
{
    "title" : "Cédric Moreau",
    "description" : "#Duniter developer",
    "city" : "Rennes",
    "socials" : [ {
      "type" : "diaspora",
      "url" : "https://diaspora-fr.org/people/f9d13420f9ff013197aa01beea1f31e2"
    } ],
    "time" : 1487422234,
    "tags" : [ "Duniter" ],
    "issuer" : "2ny7YAdmzReQxAayyJZsyVYwYhVyax2thKcGknmQy5nQ",
    "avatar" : {
      "_content_type" : "image/png",
      "_content" : "iVBORw0KGgoAAAANSUhEUgAAAGQAAABkC(...)" // base 64 encoding
    }
    "hash" : "85F527077D060E03ECAC6D1AE38A74CCC900ACAF5D52F194BA34F5A5E8A55139",
    "signature" : "WeP7JEwttAoSkHcuiFwo6N4SM0uVakTYBQ09H1+K8/nPFyxO3ak1U9EQ6qaQFoAx9IdDp5qO2EX662wP/pcEAg==",
}
```

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


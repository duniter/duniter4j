Duniter4j
=========

Duniter4j is a Java Toolkit for [Duniter](http://duniter.org).

<img src="./src/site/resources/images/logos/logo_duniter.png"/>

> See the [documentation web site](http://doc.e-is.pro/duniter4j/)

## Modules

 Duniter4j has tree main modules :
 
 - `duniter4j-client`: [a command line tool](./src/site/markdown/CLI.md), to execute basic operation on a Duniter currency: transfer, view peers, ... 
   
 - `duniter4j-core-client`: [a Java API](./src/site/markdown/Java_API.md) to help Java developers to communicate with a Duniter network.
 
 - `duniter4j-elasticsearch`:  [a ElastiSearch node](./src/site/markdown/ES.md) used to store (with full-text capabilities) all blockchain data, and additional user data. 
    
    * It comes with an [HTTP API](./src/site/markdown/ES_API.md) to store and retrieve all this data.
    
    * This API is used by [Cesium+](https://www.github.com/duniter/cesium) (a Duniter wallet).  



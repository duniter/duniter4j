duniter4j
======

duniter4j is a Java Client API for [Duniter](http://duniter.org).

## Architecture

duniter4j has four main components :

 - shared: common classes
 
 - core-client: a Client API to access to a Duniter network.
   
 - elasticsearch: a ES plugin, to store blockchain, registry, market and more.
    

## Install as ES plugin

### Install Java 

 - Install Java JRE 8 or more.
 
    - Windows: see [Oracle web site](http://oracle.com/java/index.html)
    
    - Linux (Ubuntu):
 
```bash
sudo apt-get install openjdk-8-jre 
```

 - Install [libsodium](https://download.libsodium.org/doc/index.html) (Linux only)
  
     - Linux: See [installation](https://download.libsodium.org/doc/installation/index.html). After installation, make sure the file 'libsodium.so' 
       exists on: /usr/local/lib or /opt/local/lib. If not, create a symbolic link.
       
    No installation need for Windows (include in binaries) 

### Install ElasticSearch 2.3.3

 Download lastest release of ElasticSearch
 
### Install ElasticSearch plugins
 
   /bin/plugin install mapper-attachments
   
   /bin/plugin install https://github.com/duniter/duniter4j/releases/download/0.2.0/duniter4j-elasticsearch-0.2.0.zip


## Install from standalone bundle 

 - Installa Java (see on top) 
 
 - Download lastest release of file duniter4j-elasticsearch-X.Y-standalone.zip
 
 - Unzip, then start a elasticsearch node, just do :
 
```bash
unzip duniter4j-elasticsearch-X.Y-standalone.zip
cd duniter4j-elasticsearch-X.Y
./duniter4j-elasticsearch.sh start index -h <node_host> -p <node_port>
```

Example on test_net test currency :

```bash
$ ./duniter4j-elasticsearch.sh start index -h  cgeek.fr -p 9330
2016-01-07 23:34:34,771  INFO Starting duniter4j :: ElasticSearch Indexer with arguments [start, index, -h, metab.ucoin.io, -p, 9201]
2016-01-07 23:34:34,856  INFO Application basedir: /home/user/.duniter4j-elasticsearch
2016-01-07 23:34:34,861  INFO Starts i18n with locale [fr] at [/home/user/.duniter4j-elasticsearch/data/i18n]
2016-01-07 23:34:35,683  INFO Starts ElasticSearch node with cluster name [duniter4j-elasticsearch] at [/home/user/.duniter4j-elasticsearch/data].
*** duniter4j :: Elasticsearch successfully started *** >> To quit, press [Q] or enter
2016-01-07 23:34:45,015  INFO Indexing last blocks of [test_net] from peer [cgeek.fr:9330]
2016-01-07 23:35:01,597  INFO Indexing block #999 / 47144 (2%)...
2016-01-07 23:35:15,554  INFO Indexing block #1998 / 47144 (4%)...
2016-01-07 23:35:30,713  INFO Indexing block #2997 / 47144 (6%)...
2016-01-07 23:35:45,747  INFO Indexing block #3996 / 47144 (8%)...
...
2016-01-07 23:45:00,175  INFO All blocks indexed 
```

Show help :

```bash
$ ./duniter4j-elasticsearch.sh --help

Usage: duniter4j-elaticsearch.<sh|bat> <commands> [options]

Commands:

 start                            Start elastic search node
 index                            Index blocks from BMA Node
 reset-data                       Reset indexed data for the uCoin node's currency


Options:

 --help                           Output usage information
 -h --host <user>                          uCoin node host (with Basic Merkled API)
 -p --port <pwd>                           uCoin node port (with Basic Merkled API)

 -esh  --es-host <user>           ElasticSearch node host
 -esp  --es-port <pwd>            ElasticSearch node port

```

## Use API (Developer)

When a blockchain currency has been indexed, you can test some fun queries :

 - get a block by number (e.g the block #0):
    
    http://localhost:9200/test_net/block/0 -> with some additional metadata given by ES
    
    http://localhost:9200/test_net/block/0/_source -> the original JSON block
        
 - Block #125 with only hash, dividend and memberCount:
 
    http://localhost:9200/test_net/block/125/_source?_source=number,hash,dividend,membersCount
      
 - All blocks using a pubkey (or whatever):
 
    http://localhost:9200/test_net/block/_search?q=9sbUKBMvJVxtEVhC4N9zV1GFTdaempezehAmtwA8zjKQ1
       
 - All blocks with a dividend, with only some selected fields (like dividend, number, hahs).
   Note : Query executed in command line, using CURL:

```bash
curl -XGET 'http://localhost:9200/test_net/block/_search' -d '{
"query": {
        "filtered" : {
            "filter": {
                "exists" : { "field" : "dividend" }
            }
        }
    },
    "_source": ["number", "dividend", "hash", "membersCount"]
 }'
```
        
 - Get blocks from 0 to 100 

```bash
curl -XGET 'http://localhost:9200/test_net/block/_search' -d '{
    "query": {
        "filtered" : {
            "filter": {
                "exists" : { "field" : "dividend" }
            }
        }
    }
}'
```


More documentation here :

  - ElasticSearch [official web site](http://www.elastic.co/guide/en/elasticsearch/reference/1.3/docs-get.html#get-source-filtering)
  
  - a good [tutorial](http://okfnlabs.org/blog/2013/07/01/elasticsearch-query-tutorial.html) 


## Compile from source
 
 Install required dependencies:
 
  - Install Java JDK (8 or more) 
  
  - Install [libsodium](http://doc.libsodium.org/installation/index.html)
 
    - Linux: after [installation](http://doc.libsodium.org/installation/index.html), make sure the file 'libsodium.so' exists on: /usr/local/lib or /opt/local/lib.
      If not, create a symbolic link.
 
    - Windows: copy the file 'sodium.dll' into directory 'duniter4j-core/lib/'
 
  - Install [Maven 3](http://maven.apache.org/).
```
    sudo apt-get install maven
```
 
  - Get the source code, then compile using Maven:

```
	git clone https://github.com/duniter/duniter4j.git
	cd duniter4j
	git submodule init
	git submodule sync
	git submodule update
	
    mvn install -DskipTests
```
 
 To package binaries :

```bash
$ mvn install -DskipTests -DperformRelease
```

## Roadmap

 - Allow to store data in embedded database (SQLLite or HsqlDB) 
 
 - Add an embedded [Cesium](https://www.github.com/duniter/cesium) inside the ElasticSearch plugin 

 - Detect blockchain rollback

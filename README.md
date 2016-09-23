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

 - Install Java (see on top) 
 
 - Download lastest release of file duniter4j-elasticsearch-X.Y-standalone.zip
 
 - Unzip
 
```bash
unzip duniter4j-elasticsearch-X.Y-standalone.zip
cd duniter4j-elasticsearch-X.Y/config
```

 - Edit the configuration file `config/elasticsearch.yml`, in particular this properties:

```bash
# cluster.name: my-application
cluster.name: duniter4j-elasticsearch

# Use a descriptive name for the node:
node.name: ES-NODE-1

# Set the bind address to a specific IP (IPv4 or IPv6):
network.host: 192.168.0.28

# Set a custom port for HTTP:
http.port: 9203

# Duniter node to connect with
duniter.host: cgeek.fr
duniter.port: 9330

# Should synchronize node blockchain ?
duniter.blockchain.sync.enable: true


```
 
 - Launch the node
 
```bash
cd duniter4j-elasticsearch-X.Y/bin
./elasticsearch
```

Output example (on [test_net](https://en.duniter.org/try/) currency):

```bash
$ ./elasticsearch
[2016-09-24 00:16:45,803][INFO ][node                     ] [ES-NODE-1] version[2.3.3], pid[15365], build[218bdf1/2016-05-17T15:40:04Z]
[2016-09-24 00:16:45,804][INFO ][node                     ] [ES-NODE-1] initializing ...
[2016-09-24 00:16:46,257][INFO ][plugins                  ] [ES-NODE-1] modules [reindex, lang-expression, lang-groovy], plugins [mapper-attachments, duniter4j-elasticsearch], sites [duniter4j-elasticsearch]
[2016-09-24 00:16:46,270][INFO ][env                      ] [ES-NODE-1] using [1] data paths, mounts [[/home (/dev/mapper/isw_defjaaicfj_Volume1p1)]], net usable_space [1tb], net total_space [1.7tb], spins? [possibly], types [ext4]
[2016-09-24 00:16:46,270][INFO ][env                      ] [ES-NODE-1] heap size [989.8mb], compressed ordinary object pointers [true]
[2016-09-24 00:16:47,757][INFO ][node                     ] [ES-NODE-1] initialized
[2016-09-24 00:16:47,757][INFO ][node                     ] [ES-NODE-1] starting ...
[2016-09-24 00:16:47,920][INFO ][transport                ] [ES-NODE-1] publish_address {192.168.0.5:9300}, bound_addresses {192.168.0.5:9300}
[2016-09-24 00:16:47,924][INFO ][discovery                ] [ES-NODE-1] duniter4j-elasticsearch/jdzzh_jUTbuN26Enl-9whQ
[2016-09-24 00:16:50,982][INFO ][cluster.service          ] [ES-NODE-1] detected_master {EIS-DEV}{FD0IzkxETM6tyOqzrKuVYw}{192.168.0.28}{192.168.0.28:9300}, added {{EIS-DEV}{FD0IzkxETM6tyOqzrKuVYw}{192.168.0.28}{192.168.0.28:9300},}, reason: zen-disco-receive(from master [{EIS-DEV}{FD0IzkxETM6tyOqzrKuVYw}{192.168.0.28}{192.168.0.28:9300}])
[2016-09-24 00:16:53,570][INFO ][http                     ] [ES-NODE-1] publish_address {192.168.0.5:9203}, bound_addresses {192.168.0.5:9203}
[2016-09-24 00:16:53,570][INFO ][node                     ] [ES-NODE-1] started
[2016-09-24 00:16:57,850][INFO ][node                     ] Checking Duniter indices...
[2016-09-24 00:16:57,859][INFO ][node                     ] Checking Duniter indices... [OK]
[2016-09-24 00:17:08,026][INFO ][duniter.blockchain       ] [test_net] [cgeek.fr:9330] Indexing last blocks...
[2016-09-24 00:17:08,026][INFO ][duniter.blockchain       ] [test_net] [cgeek.fr:9330] Indexing block #999 / 41282 (2%)...
[2016-09-24 00:17:08,045][INFO ][duniter.blockchain       ] [test_net] [cgeek.fr:9330] Indexing block #1998 / 41282 (4%)...
[2016-09-24 00:17:09,026][INFO ][duniter.blockchain       ] [test_net] [cgeek.fr:9330] Indexing block #2997 / 41282 (6%)...
[2016-09-24 00:17:10,057][INFO ][duniter.blockchain       ] [test_net] [cgeek.fr:9330] Indexing block #3996 / 41282 (8%)...
...
[2016-09-24 00:17:11,026][INFO ][duniter.blockchain       ] [test_net] [cgeek.fr:9330] Indexing block #41282 - hash [00000AAD73B0E76B870E6779CD7ACCCE175802D7867C13B5C8ED077F380548C5]
```

Show help :

```bash
$ ./elasticseacrh --help
NAME

    start - Start Elasticsearch

(...)

OPTIONS

    -h,--help                    Shows this message

    -p,--pidfile <pidfile>       Creates a pid file in the specified path on start

    -d,--daemonize               Starts Elasticsearch in the background

    -Dproperty=value             Configures an Elasticsearch specific property, like -Dnetwork.host=127.0.0.1

    --property=value             Configures an elasticsearch specific property, like --network.host 127.0.0.1
    --property value

    NOTE: The -d, -p, and -D arguments must appear before any --property arguments.

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


## Troubleshooting

### Could not find an implementation class.

Message:

```
java.lang.RuntimeException: java.lang.RuntimeException: Could not find an implementation class.
        at org.duniter.core.util.websocket.WebsocketClientEndpoint.<init>(WebsocketClientEndpoint.java:56)
        at org.duniter.core.client.service.bma.BlockchainRemoteServiceImpl.addNewBlockListener(BlockchainRemoteServiceImpl.java:545)
        at org.duniter.elasticsearch.service.BlockchainService.listenAndIndexNewBlock(BlockchainService.java:106)
```

Cause:

Plugin use Websocket to get notification from a Duniter nodes. The current library ([Tyrus](https://tyrus.java.net/)) is loaded throw java Service Loader, that need access to file `META-INF/services/javax.websocket.ContainerProvider` contains by Tyrus.
ElasticSearch use separated classloader, for each plugin, that disable access to META-INF resource.

Solution :

Move Tyrus libraries into elasticsearch `lib/` directory :

```
    cd <ES_HOME>
    mv plugins/duniter4j-elasticsearch/tyrus-*.jar lib
    mv plugins/duniter4j-elasticsearch/javax.websocket-api-*.jar lib
```
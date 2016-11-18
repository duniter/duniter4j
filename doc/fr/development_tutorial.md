## Introduction

Cet article est un tutoriel d'initiation au code source du logiciel Duniter4j.
Celui-ci vous permettra d'installer l'environnement de développement, et compiler et de lancer un noeud ElasticSearch avec le plugin Duniter4j.

A la fin de ce tutoriel, vous serez donc *capable de modifier le logiciel*. 

## Rappel d'architecture

Le projet Duniter4j est composé de 3 sous-modules :

 - `duniter4j-core-shared`: Classes utilitaires Java. Réutilisable dans d'autres projets Java autour de Duniter.
 
 - `duniter4j-core-client`: Ensemble de services Java permettant d'accéder à un réseau Duniter (c'est à dire une API Java client Duniter) . Cette partie est **réutilisable dans d'autres applications Java**.
   
 - `duniter4j-elasticsearch`: Il s'agit d'un plugin ElasticSearch, qui implémente : 
  * l'API ES (indexation de blockchain);
  * l'API Cesium+ (gestion de profils, des messages privées);
  * l'API GChange (annonces, annuaire des profesionnels). Note : cette partie sera pas la suite sortie dans un autre plugin/projet. 

## Niveau I : récupérer le code source

Ce premier niveau consiste à créer *votre propre version* des sources du logiciel et de récupérer cette copie sur votre ordinateur. Vous y produirez : 

* votre propre compte *GitHub*
* votre propre version du logiciel, votre *fork*
* une copie locale des fichiers de code source provenant de votre *fork*

### Créez un compte GitHub

> Si vous disposez déjà d'un compte GitHub, vous pouvez passer cette étape.

Rendez-vous sur https://github.com (site en anglais). Renseigner les 3 champs proposés :

* Nom d'utilisateur
* E-mail
* Mot de passe

<img src="https://forum.duniter.org/uploads/default/original/1X/13ade346327b73bbf1acc97027af147eeb4e9089.png" width="346" height="325">

Vous recevrez probablement un e-mail de confirmation qu'il vous faudra valider. Une fois cette étape passée, vous devriez disposer d'un compte GitHub .

### Forkez le dépôt principal

Rendez-vous à l'adresse https://github.com/duniter/duniter4j. Cliquez sur le bouton « Fork » en dans le coin supérieur droit de la page :

<img src="https://forum.duniter.org/uploads/default/original/1X/3b9228c664520496d6a7e86e3f9c4c438f111914.png" width="388" height="98">

### Installer Git

L'installation de Git dépend de votre système d'exploitation. Suivez simplement les indications présentes sur : https://git-scm.com/

### Cloner votre fork

A ce stade, vous êtes en mesure de récupérer votre version du code source (votre *fork*), afin de pouvoir travailler dessus.

#### Ouvrez Git en ligne de commande

Pour récupérer le code source, lancez Git en mode console.

* Sous Linux et MacOS, ouvrez tout simplement le Terminal
* Sous Windows lancez le programme *Git Bash* :

<img src="https://forum.duniter.org/uploads/default/original/1X/6fc638dc0a22d88da7e84dbf0371e69747767f78.png" width="432" height="80">

#### Clonez votre fork, en ligne de commande

Retournez sur la page web GitHub, puis trouvez le bouton « Clone or download » : 
Cliquez dessus, vous pourrez alors copier l'URL de clonage en cliquant sur l'icône de valise.

Vous n'avez plus qu'à retourner dans votre console Git et saisir : 

    git clone <coller l'URL copiée>

ce qui donne dans mon cas : 

```
git clone https://github.com/blavenie/duniter4j.git
Cloning into 'duniter4j'...
 (...)
Checking connectivity... done.
```

Si vous êtes arrivés à un comportement similaire, **bravo**, vous posséder désormais le code source du projet !

## Niveau II : Compilation et lancement

Ce second niveau vise à obtenir les outils de base pour exécuter le code source, et vérifier son bon fonctionnement. Vous y réaliserez : 

* l'installation de Java Development Kit (JDK);
* la compilation du code;
* la vérification du bon fonctionnement du code source *via* le lancement de l'application, dans un terminal.

Si l'application se lance, vous aurez dores et déjà un environnement entièrement **fonctionnel** !

### Installer JDK

 - Sous Windows : Téléchargez puis installez un JDK (version 8 ou +) depuis le [site web d'Oracle ](http://oracle.com/java/index.html)
 - Sous Linux (Debian) : Lancez la commande suivante :

```bash
sudo apt-get install openjdk-8-jre 
```

### Installer LibSodium

[libsodium](https://download.libsodium.org/doc/index.html) est une librairie de cryptographie.

 - Sous Windows : Aucune instalation nécessaire (fichier `sodium.dll` déjà présent dans `duniter4j-core-shared/lib`);
 
 - Sous Linux : suivre [les notes d'installation](https://download.libsodium.org/doc/installation/index.html) (anglais).
    * Après installation, vérifiez que le fichier `libsodium.so` existe bien dans `/usr/local/lib` ou `/opt/local/lib`.
    * S'il existe, mais à une autre eplacement, veuillez créez un lien symbolique à l'un ou l'autre de ces emplacements.


### Installer Apache Maven 3

Installer les outils nécessaires pour la compilation :

  - Installez [Apache Maven 3](http://maven.apache.org]
    * Sous Windows : [téléchargez](http://maven.apache.org/download.cgi) (version 3.x) puis installez en suivant [ces instructions]((http://maven.apache.org/install.html).
    * Sous Linux : Lancez la commande :
```
    sudo apt-get install maven
```

### Installer un IDE

Pour développer en Java, vous pouvez utiliser l'IDE de votre choix, par exemple :

 * Sublime Text (non libre) : https://www.sublimetext.com/
 * Autre possibilité : [Idea](https://www.jetbrains.com/idea/download/) (non libre mais fonctionnement très avancé).

## Niveau III : maîtriser les commandes usuelles

Ce troisième niveau permet de découvrir les quelques commandes que vous utiliserez tout le temps si vous développez sur Duniter4j. Vous y apprendrez : 

* à configurer le projet, notamment les paramètres réseau (du noeud ES, du noeud Duniter, etc.);
* à compiler le projet;
* à lancer votre noeud ElasticSearch avec le plugin Duniter4j;

### Configurer le projet

La configuration utilisée pour le développement est visible dans le fichier : `/duniter4j-elasticsearch/src/test/es-home/config/elasticsearch.yml`

#### Configuration du noeud Duniter

Si vous avez un noeud Duniter qui est lancé localement, configurez le en modifiant les propriétés suivantes :
 
```yml
#
# Duniter node to synchronize
#
duniter.host: localhost
duniter.port: 8999    <- à remplacer par le port de votre noeud
```

Si vous n'avez pas de noeud local, conservez la configuration par défaut :

```yml
#
# Duniter node to synchronize
#
duniter.host: cgeek.fr
duniter.port: 9330
```

#### Désactivation de la couche de sécurité

Duniter4j a une couche de sécurité, qui empêche l'appel à des URL non autorisées. 
Nous allons **désactiver cette couche de sécurité** pour faciliter l'apprentissage qui va suivre.

Modifiez comme suit, modifier le fichier de configuration `duniter4j-elasticsearch/src/main/assembly/config/elasticsearch.yml` :

```bash
duniter.security.enable: false
```

### Compiler le projet

La compilation du projet utilise Maven, par la commande  `mvn` (à ne pas confondre avec `nvm`).

#### Tout compiler

Executez la commande suivante pour compiler l'ensemble du projet : 
```bash
mvn install
```

Si tout c'est bien passé, vous devriez obtenir quelque chose qui ressemble à cela : 
```bash
(...)
[INFO] Building zip: /home/user1/git/duniter/duniter4j/duniter4j-elasticsearch/target/duniter4j-elasticsearch-0.3.5-SNAPSHOT-standalone.zip
[INFO] 
[INFO] --- maven-install-plugin:2.4:install (default-install) @ duniter4j-elasticsearch ---
[INFO] Installing /home/user1/git/duniter/duniter4j/duniter4j-elasticsearch/target/duniter4j-elasticsearch-0.3.5-SNAPSHOT.jar to /home/user1/.m2/repository/org/duniter/duniter4j-elasticsearch/0.3.5-SNAPSHOT/duniter4j-elasticsearch-0.3.5-SNAPSHOT.jar
[INFO] Installing /home/user1/git/duniter/duniter4j/duniter4j-elasticsearch/pom.xml to /home/eis/.m2/repository/org/duniter/duniter4j-elasticsearch/0.3.5-SNAPSHOT/duniter4j-elasticsearch-0.3.5-SNAPSHOT.pom
[INFO] Installing /home/user1/git/duniter/duniter4j/duniter4j-elasticsearch/target/duniter4j-elasticsearch-0.3.5-SNAPSHOT.zip to /home/user1/.m2/repository/org/duniter/duniter4j-elasticsearch/0.3.5-SNAPSHOT/duniter4j-elasticsearch-0.3.5-SNAPSHOT.zip
[INFO] Installing /home/user1/git/duniter/duniter4j/duniter4j-elasticsearch/target/duniter4j-elasticsearch-0.3.5-SNAPSHOT-standalone.zip to /home/user1/.m2/repository/org/duniter/duniter4j-elasticsearch/0.3.5-SNAPSHOT/duniter4j-elasticsearch-0.3.5-SNAPSHOT-standalone.zip
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO] 
[INFO] Duniter4j : a Duniter Java Client API .............. SUCCESS [  0.611 s]
[INFO] Duniter4j :: Core Shared ........................... SUCCESS [ 11.141 s]
[INFO] Duniter4j :: Core Client API ....................... SUCCESS [ 13.635 s]
[INFO] Duniter4j :: ElasticSearch Plugin .................. SUCCESS [ 23.081 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 48.733 s
[INFO] Finished at: 2016-11-17T11:06:16+01:00
[INFO] Final Memory: 74M/741M
[INFO] ------------------------------------------------------------------------
```

Bravo, vous avez compiler avec succès le projet !


#### Compiler sans les tests unitaires

Par la suite, vous pourrez **ignorer les tests unitaires**, de cette manière : 
```bash
mvn install -DskipTests
```
Cela permet une compilation plus rapide.
 
### Lancer un noeud ElasticSearch

Il ne vous reste plus qu'à lancer un noeud local ElasticSearch, intégrant le plugin Duniter4j.

Lancez la commande suivante : 

```bash
mvn install -Prun -pl duniter4j-elasticsearch
```

Vous devriez avoir maintenant :

```bash
[2016-11-17 13:29:35,874][INFO ][transport                ] [Att-Lass] publish_address {127.0.0.1:9300}, bound_addresses {[::1]:9300}, {127.0.0.1:9300}
[2016-11-17 13:29:35,882][INFO ][discovery                ] [Att-Lass] duniter4j-elasticsearch-TEST/HjDNQrTTSuyfmzGEbY7qNQ
[2016-11-17 13:29:38,956][INFO ][cluster.service          ] [Att-Lass] new_master {Att-Lass}{HjDNQrTTSuyfmzGEbY7qNQ}{127.0.0.1}{127.0.0.1:9300}, reason: zen-disco-join(elected_as_master, [0] joins received)
[2016-11-17 13:29:38,990][INFO ][http                     ] [Att-Lass] publish_address {127.0.0.1:9200}, bound_addresses {[::1]:9200}, {127.0.0.1:9200}
[2016-11-17 13:29:38,991][INFO ][node                     ] [Att-Lass] started
[2016-11-17 13:29:39,515][INFO ][gateway                  ] [Att-Lass] recovered [6] indices into cluster_state
[2016-11-17 13:29:41,655][INFO ][cluster.routing.allocation] [Att-Lass] Cluster health status changed from [RED] to [YELLOW] (reason: [shards started [[registry][1], [registry][1]] ...]).
[2016-11-17 13:29:45,756][INFO ][node                     ] Checking Duniter indices...
[2016-11-17 13:29:45,766][INFO ][node                     ] Checking Duniter indices... [OK]
[2016-11-17 13:29:58,052][INFO ][duniter.blockchain       ] [test_net] [cgeek.fr:9330] Indexing last blocks...
```

### Vérifier le fonctionnement

Pour vérifier le bon fonctionnement de votre noeud ES, ouvrez un navigateur à l'adresse suivante : http://127.0.0.1:9200  (il s'agit de l'adresse par défaut d'ElasticSearch).

Vous devriez voir le contenu suivant : 
<img src="https://forum.duniter.org/uploads/default/original/2X/f/fb3d42bb463334b4223d40c1a45a16caa8f589a2.png" width="506" height="260">

Bravo, votre noeud ElasticSearch Duniter4j est fonctionnel !

### En cas de problème au lancement

#### Erreur `BindTransportException[Failed to bind to [9300-9400]];`

Cette erreur indique qu'ElasticSearch n'a pas pu démarrer sur le port demandé.

Vous pouvez changer de port (ou l'IP), en éditant les propriétés suivantes du fichier `/duniter4j-elasticsearch/src/test/es-home/config/elasticsearch.yml` : 

```bash
# ---------------------------------- Network -----------------------------------
#
# Set the bind address to a specific IP (IPv4 or IPv6):
#
network.host: 192.168.233.1    <-- Remplacez par votre IP
#
# Set a custom port for HTTP:
#
http.port: 9200   <-- Remplacez par un port libre de votre machine (plage 9200-9300 par défaut)
```

## Niveau IV : Se repérer dans le code 

### Répérer les couches logicielles

Ouvrir votre IDE, et ouvrir le projet Duniter4j.

Dans le répertoire `duniter4j-elasticsearch/src/main/java`, cherchez et répérez dans le code : 

- les controlleurs REST : package `org.duniter.elasticsearch.action`
- les services d'indexation : package `org.duniter.elasticsearch.service`.
  * Il existe un service d'indexation par type de stockage. Par exemple : `BlockchainService`, `UserService`, etc.

Dans le répertoire `duniter4j-core-client/src/main/java`, cherchez et répérez dans le code : 

* les services d'accès au réseau Duniter : package `org.duniter.core.client.service.bma`

### Aller plus loin dans le code

Duniter4j s'appuie sur ElasticSearch **en version 2.3**. D'excellentes documentations sont présentes sur le web : https://www.elastic.co/guide/en/elasticsearch/reference/2.3/index.html.

## Niveau V : Requêtage sur ES API

Nous allons requeter l'indexation de la BlockChain `test_net`, qui s'est fait dès le démarrage de votre noeud ElastiSearch. Nous appellons cette indexation l'**ES API**. 

Il existe plusieurs manière de requéter un noeud ES : 

 - Requêtes HTTP GET
 - Requêtes HTTP POST

### Requêtes GET

En utilisant un navigateur, vous allez requêter .

- [GET-1] Visualisez un bloc quelconque (par exemple le premier #0): http://localhost:9200/test_net/block/0
  
Etudiez ensuite le format du résultat : 
```json
{"_index":"test_net","_type":"block","_id":"0","_version":1,"found":true,"_source":{
 ...
}
```
> Observez qu'ElasticSeach a ajouté des informations : `_index`, `_type`, etc.

- [GET-2] Pour éviter d'avoir les informations additionnelles, ajoutez `/_source` : http://localhost:9200/test_net/block/0/_source

> Notez que le bloc courant est accessible en `/test_net/block/current`
        
- [GET-3] Récupérer **uniquement** les champs `hash`, `dividend` et `memberCount`, pour le bloc #125 : http://localhost:9200/test_net/block/125/_source?_source=number,hash,dividend,membersCount

> Notez que vous pouvez avoir une meilleure présentation en ajoutant "`&pretty`" dans l'adresse;

- [GET-4] Les blocks qui référence une clef publique (recherche full text) : http://localhost:9200/test_net/block/_search?q=8Fi1VSTbjkXguwThF4v2ZxC5whK7pwG2vcGTkPUPjPGU

> Vous pouvez rechercher sur n'importe quelle chaine (recherche `full-text`), via cet option "`q=`" 

### Requêtes via HTTP POST

Les requetes de type POST permettent des filtrage beaucoup plus fins. C'est généralement ce type de requêtes qui est utilisées dans des logiciels clients. On peut par exemple ajouter une mise en surbrillance; requêter plusieurs index en une seule fois, etc.

Nous utiliserons `curl` pour requeter notre noeud ElasticSearch.
Si vous ne l'avez pas encore installé, executer simplement (sous Linux) :

```bash
sudo apt-get install curl
```

Dans un terminal, exécuter les commandes suivantes :

- [POST-1] Récupérez les blocs ayant un dividende universel, en sélectionnant **quelques champs** uniquement (dividend, number, hash). : 
```bash
curl -XGET 'http://localhost:9200/test_net/block/_search?pretty' -d '{
"query": {
        "filtered" : {
            "filter": {
                "exists" : { "field" : "dividend" }
            }
        }
    },
    "_source": ["number", "dividend", "hash", "membersCount"],
   size: 10000
 }'
```
        
- [POST-2] Récupérez tous les blocs de #0 à #100 :

```bash
curl -XGET 'http://localhost:9200/test_net/block/_search' -d '{
    "query": {
       "bool": {
           "should": {
                "range":{
                    "number":{
                        "lte":100
                    }
                }
            }
       }
    }
}'
```

### Toute la doc pour aller plus loin dans les requetes

Voici la documentation pour aller plus loin :

- ElasticSearch [official web site](http://www.elastic.co/guide/en/elasticsearch/reference/1.3/docs-get.html#get-source-filtering)
- un bon [tutoriel](http://okfnlabs.org/blog/2013/07/01/elasticsearch-query-tutorial.html) 

## Niveau VI : Requêtage sur Cesium+ API et Ğchange API

Duniter4j permet aussi de stocker et d'indexer les données hors BlockChain, comme celles utilisées par Cesium+ et ĞChange : 

- `/user/profile` : les profiles utilisateurs (nom complet, réseaux sociaux, avatar, etc.)   
- `/message/record` : les messages privées envoyés    
- `/market/record` : les annonces de la [place de maché](http://cesium.duniter.fr/#/app/market/lg) Ğchange;   
  * `/market/record` : les commentaires sur les annonces
- `/registry/record` : les référencement de l'[annuaire pro](http://cesium.duniter.fr/#/app/registry/lg) Ğchange;   
  * `/market/comment` : les commentaires sur les référencements

### Requêtes sur `data.duniter.fr`

> **Note** : ce noeud a activer la couche de sécurité duniter4j. Les accès sur des URL non autorisés renverront une page vide  

#### Requêtes GET

- [GET-5] Liste des annonces avec le mot `rml8` : http://data.duniter.fr/market/record/_search?pretty&q=rml8

- [GET-5] Liste des messages à destination de la clef `` : data.duniter.fr/message/record/_search?pretty&receiver=5ocqzyDMMWf1V8bsoNhWb1iNwax1e9M7VTUN6navs8of

> Observez que le contenu des messages est chiffré. Seul sont destinataire peut y accéder.


#### Requêtes POST


- [POST-3] Liste des [annonces soumises par Sybille](http://cesium.duniter.fr/#/app/market/lg?q=GzfTB21AnTmbVfNANZmPGSXVtYE4YNnvA9E9ovokttZr) (pubkey=`GzfTB21AnTmbVfNANZmPGSXVtYE4YNnvA9E9ovokttZr`)

```bash
curl -XPOST "http://data.duniter.fr/market/record/_search?pretty" -d'
{
  "query": {
        
        "bool":{
            "should": {
                "range":{
                    "price":{
                        "gte":0
                    }
                }
            },
            "filter": [
                {"term":{
                        "issuer":"GzfTB21AnTmbVfNANZmPGSXVtYE4YNnvA9E9ovokttZr"
                    }
                }                
            ]
        }
  },
  "_source": ["title", "description", "price", "time", "unit", "type"],
  "from":0,
  "size":100
}'
```

Normalement, vous devriez récupérer **une annonce**.

> **Note** : Dans Cesium (dans la partie qui deviendra ĞChange), ce même contenu est visible ici : http://cesium.duniter.fr/#/app/market/lg?q=GzfTB21AnTmbVfNANZmPGSXVtYE4YNnvA9E9ovokttZr  
> Notez que l'option dans cette adresse utilise `q=` pour la recherche `full text`.

Copiez la valeur du champ `_id`, et utilisez la dans la prochaine requête :

 - [POST-4] : Récupérez les commentaires [de l'annonce précédente](http://cesium.duniter.fr/#/app/market/view/AVbieTIAup9uzWgKipsC/animation-geconomicus) : 
```bash
curl -XPOST "http://data.duniter.fr/market/comment/_search?pretty" -d'
{
  "query": {
        "bool":{
            "filter": [
                {"term":{
                        "record": "  ...   "   <-- mettre ici l'identifiant (id) de l'annonce
                    }
                }
            ]
        }
  }
}'
```

> **Note** : Dans Cesium (dans la partie qui deviendra ĞChange), le contenu de cette requete est visible ici : [http://cesium.duniter.fr/#/app/market/view/AVbieTIAup...](http://cesium.duniter.fr/#/app/market/view/AVbieTIAup9uzWgKipsC/animation-geconomicus/AVbkYjuR?u=kimamila)
> (notez que l'identifiant de l'annonce est également présent dans cette adresse).

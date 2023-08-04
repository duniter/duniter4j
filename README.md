Duniter4j
=========

Duniter4j is a Java Toolkit for [Duniter](http://duniter.org).

<img src="./src/site/resources/images/logos/logo_duniter.png"/>

> See the [documentation web site](http://doc.e-is.pro/duniter4j/)

## Manual

- Install Java JRE (1.8 or higher)
- Download the file `duniter4j-client-vX.Y.Z.zip` from the [latest releases page](https://www.github.com/duniter/duniter4j/releases)
- Unzip the archive;
- The open a terminal and execute the script `duniter4j.sh` (or `duniter4j.bat`) :
```bash
 cd duniter4j-client-vX.Y.Z
 ./duniter4j.sh --help
```


## Architecture

 Duniter4j has tree main modules :
 
- `duniter4j-core-shared`: A set of useful classes, used by other modules.
- `duniter4j-core-client`: [a Java API](./src/site/markdown/Java_API.md) to help Java developers to communicate with a Duniter network.
- `duniter4j-client`: [a command line tool](./src/site/markdown/CLI.md), to execute basic operation on a Duniter currency: transfer, view peers, ...


## Build from sources

- Install Apache Maven (3.1.1+) 
- Run the build command:
```bash
mvn install
```

## Use as Maven dependency

```xml

<dependencies>
  <!-- Duniter4j dependency -->
  <dependency>
    <groupId>org.duniter</groupId>
    <artifactId>duniter4j-core-client</artifactId>
    <version>x.y.z</version> <!-- -->
  </dependency>
</dependencies>

<!-- Duniter4j repository -->
<repositories>
    <repository>
      <id>duniter4j-public-group</id>
      <url>https://nexus.e-is.pro/nexus/content/groups/duniter4j</url>
    </repository>
</repositories>
```


## Create a new release

```bash
./release.sh
```
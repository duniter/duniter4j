
# Build from source

## Get source and compile
 
Install required dependencies:
 
- Install Java JDK (8 or more) 

- Install [libsodium](http://doc.libsodium.org/installation/index.html)

   * Linux: after [installation](http://doc.libsodium.org/installation/index.html), make sure the file 'libsodium.so' exists on: /usr/local/lib or /opt/local/lib.
     If not, create a symbolic link.

   * Windows: copy the file 'sodium.dll' into directory 'duniter4j-core/lib/'

- Install [Maven 3](http://maven.apache.org/): `sudo apt-get install maven`

- Get the source code, then compile using Maven:

```
git clone https://github.com/duniter/duniter4j.git
cd duniter4j
git submodule init
git submodule sync
git submodule update

mvn install -DskipTests
```
 
- Then, package all binaries:

```bash
mvn install -DskipTests -DperformRelease
```

## Deploy binaries

You will need to have access to project site repository. 

To deploy binaries:

```bash
mvn release:prepare
mvn release:perform
```

## Deploy the web site

To deploy the web site:

```bash
mvn site-deploy -DperformRelease
```

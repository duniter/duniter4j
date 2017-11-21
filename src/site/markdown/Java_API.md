
# Java API


## Maven dependency

In your `pom.xml` file, add this dependency:

```xml

<dependencies>
    <!-- (...) -->
    
    <dependency>
          <groupId>org.duniter</groupId>
          <artifactId>duniter4j-core-client</artifactId>
          <version>${project.version}</version>
    </dependency>

</dependencies>
```

## Example

Using Duniter4j is simple. Here a basic example :

```java
public class Example {
    
    // ...
    
    public static void main(String[] args) {
        
       // Init configuration
       String configFilename = "duniter4j-config.properties";
       Configuration config = new Configuration(configFilename, args);
       Configuration.setInstance(config);
       
       // Set a wallet id (an identifier required for cache)
       ServiceLocator.instance().getDataContext().setAccountId(0);

       // Initialize service locator
       ServiceLocator.instance().init();
       
       // Create a peer, from configuration
       Peer aPeer = Peer.newBuilder()
               .setHost(config.getNodeHost())
               .setPort(config.getNodePort())
               .build();

       // Get the current block !
       BlockchainBlock currentBlock = ServiceLocator.instance().getBlockchainRemoteService().getCurrentBlock(aPeer);
       System.out.println(String.format("Hello %s world !", currentBlock.getCurrency()));
       
       // Let's do something else ?
    }
}
```

> See [the full example](./xref/org/duniter/core/client/example/Example1.html).

## Configuration

  Java API use Nuiton-config to manage options.
  
  See [all available options](./config-report.html) in the configuration file.
  
    
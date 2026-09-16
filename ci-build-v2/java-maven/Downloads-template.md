[WARNING]: # (Downloads.md is auto-generated from Downloads_template.md. DO NOT CHANGE the former!)

# rdf2pg Tools Donwloads

## rdf2neo

* [Latest Stable Release](%rdf2neoRel%)
* [Latest Dev Release](%rdf2neoSnap%)

## rdf2graphml

* [Latest Stable Release](%rdf2graphmlRel%)
* [Latest Dev Release](%rdf2graphmlSnap%)

## Including rdf2pg tools in your project

We use Maven and we have our own repository for Maven artifacts. So, to use our tools 
programmatically you can point to them in your POM this way:

```xml
<pom>
...
	<!-- For rdf2neo -->
	<dependency>
		<groupId>uk.ac.rothamsted.kg</groupId>
		<artifactId>rdf2neo</artifactId>
		<version>SEE POM AT https://github.com/Rothamsted/rdf2pg/blob/master/rdf2neo/pom.xml</version>
	</dependency>

	<!-- For rdf2graphml -->
	<dependency>
		<groupId>uk.ac.rothamsted.kg</groupId>
		<artifactId>rdf2graphml</artifactId>
		<version>SEE POM AT https://github.com/Rothamsted/rdf2pg/blob/master/rdf2graphml/pom.xml</version>
	</dependency>
...
	<repositories>
	  <repository>
	  	<!-- Where the artifacts above are -->
			<id>knetminer-repo</id>
			<name>Knetminer Unified Repository</name>
			<url>https://knetminer.com/artifactory/repository/maven-public</url>
			<snapshots><enabled>true</enabled></snapshots>
			<releases><enabled>true</enabled></releases>
			  </repository>
	</repositories>	
```

The entry point to start a conversion (like the command line tools do) is 
[`MultiConfigPGMaker#getSpringInstance()`][10]. For instance, in order get an instance of the Neo4j converter,
you should include the [rdf2neo][20] in your project/classpath, and then: 

```java
try ( MultiConfigNeo4jLoader mloader = MultiConfigNeo4jLoader.getSpringInstance ( 
  <config XML path>, MultiConfigNeo4jLoader.class );
)
{
  mloader.load ( <TDB path> );
}		
```

Where the XML configuration defines the RDF-to-PG mapping and other, target-depending aspects (eg, the Neo4j 
connection coordinates), and the TDB path is the RDF input (as a Jena TDB database).  


See [here][100] for details on the framework architecture.

[20]: https://github.com/Rothamsted/rdf2pg/tree/master/rdf2neo
[10]: https://github.com/Rothamsted/rdf2pg/blob/master/rdf2pg-core/src/main/java/uk/ac/rothamsted/kg/rdf2pg/pgmaker/MultiConfigPGMaker.java
[100]: https://github.com/Rothamsted/rdf2pg/wiki/rdf2pg-Architecture

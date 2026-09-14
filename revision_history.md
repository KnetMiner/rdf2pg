# Revision History

* *This file has been last revised on 2026-09-14*. **Please, keep this note updated**.

## 7.0.2-SNAPSHOT
* `GeneralConfig`, introduced, to manage app-level config properties.
* 'Big values mode' added to the general configuration.
* Utils:
	* reactive paginated read
	* existing read utilites made more efficient
* Test fixture: migration from `com.github.harti2006:neo4j-server-maven-plugin` to TestContainers.
* Various dependency upgrades.


## 7.0.1
* rdfutils dependency upgrade.


## 7.0
* `Neo4jUtils.executeReadWithTimeout` added.
* `rdfutils-jena` dependency updated, which implies Jena dependency update.
* Neo4j dependencies updated.


## 6.0
* Migration to the new AWS Maven repository.
* `XNeo4jDriver` added.
* `GenericNeo4jException` deprecated (see comments).
* `Neo4jReactorUtils` added.
* Dependencies upgraded.
* Logger configuration improved.
* paginated reads added to `neo4j-utils`.

## 5.1
* Code cleaning and improvement.
* Various dependency upgrades.


## 5.0
* Migrated to Java 17. **WARNING: no backward compatibility guaranteed**.


## 4.1
* Generalisation of the indexing functionality, with default simple TSV indexer
* Complete review of the PG maker options, turned into proper maps.


## 4.0
* **Turned into rdf2pg**. Now it has a new architecture, which makes it easy to write 
  converters from RDF to any property graph endpoint, by means of RDF/PG mapping.
* **Requires JDK >= 11 from this version**
* Some dependencies upgraded.
 
 
## 1.0.1
* Made to link stable releases of our own dependencies.


## 1.0
* First release.
package uk.ac.rothamsted.kg.rdf2pg.neo4j.test;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

/**
 * Utilities needed by tests.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>2 Dec 2020</dd></dl>
 *
 */
public class NeoTestUtils
{
	/**
	 * Facility to empty the Neo4j test DB.
	 */
	public static void initNeo ( Driver neoDriver)
	{
		try (	Session session = neoDriver.session () )
		{
			session.run ( "MATCH (n) DETACH DELETE n" );
		}
	}
}

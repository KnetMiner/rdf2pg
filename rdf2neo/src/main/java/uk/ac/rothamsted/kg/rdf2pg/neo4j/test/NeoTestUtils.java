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
	public static final String NEO_TEST_URL = "bolt://127.0.0.1:17690";
	public static final String NEO_TEST_USER = "neo4j";
	public static final String NEO_TEST_PWD = "testTest";
	
	
	/**
	 * Facility to empty the Neo4j test DB.
	 */
	public static void initNeo ()
	{
		try (	
				Driver neoDriver = GraphDatabase.driver( NEO_TEST_URL, AuthTokens.basic ( NEO_TEST_USER, NEO_TEST_PWD ) );
				Session session = neoDriver.session ();
			)
		{
			session.run ( "MATCH (n) DETACH DELETE n" );
		}
	}

	
	public static Driver getNeoDriver ()
	{
		return GraphDatabase.driver( NEO_TEST_URL, AuthTokens.basic ( NEO_TEST_USER, NEO_TEST_PWD ) );
	}
}

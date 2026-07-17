package uk.ac.rothamsted.kg.rdf2pg.neo4j.cli;

import org.junit.Test;
import org.neo4j.driver.GraphDatabase;

import uk.ac.rothamsted.kg.rdf2pg.cli.Rdf2PGCli;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.MultiConfigNeo4jLoader;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.Neo4jGeneralConfig;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.test.NeoTestUtils;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.GeneralConfig;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.MultiConfigPGMaker;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.rdf.RdfDataManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Tests the big values options in {@link Neo4jGeneralConfig}.
 *
 * @author Marco Brandizi
 * <dl><dt>Date:</dt><dd>15 Jul 2026</dd></dl>
 *
 */
public class BigValuesIT
{
	/**
	 * The default config in rdf2neo is {@link Neo4jGeneralConfig} 
	 */
	@Test
	public void testRightDefaultConfig ()
	{
		try (
			var maker = MultiConfigPGMaker.getSpringInstance (
				"target/test-classes/test_config.xml", MultiConfigNeo4jLoader.class
			)
		)
		{
			var config = maker.getSpringContext ().getBean ( GeneralConfig.class );
			assertTrue ( "Wrong config bean loaded!", config instanceof Neo4jGeneralConfig );
		}
	}
	
	/**
	 * The default config is overridden by the XML config.
	 */
	@Test
	public void testXMLConfig ()
	{
		try (
			var maker = MultiConfigPGMaker.getSpringInstance (
				"target/test-classes/big-values/config.xml", MultiConfigNeo4jLoader.class
			)
		)
		{
			GeneralConfig config = maker.getSpringContext ().getBean ( RdfDataManager.class )
				.getGeneralConfig ();
			assertTrue ( "Wrong config bean loaded!", config instanceof Neo4jGeneralConfig );
			assertEquals ( "maxStringLength not overriden by the XML!", 50, config.getMaxStringLength () );
			assertEquals ( "Wrong maxSetSize not overridden by the XML!", 20, config.getMaxSetSize () );
		}
	}
	
	/**
	 * As per {@link GeneralConfig.BigValueMode#TRUNCATE}, a single value longer than the configured
	 * max len is truncated. 
	 */
	@Test
	public void testBigSingleStringTruncation ()
	{
		int maxStrLen = 0;
		String truncTrailer = null;
		
		try (
			var maker = MultiConfigPGMaker.getSpringInstance (
				"target/test-classes/big-values/config.xml", MultiConfigNeo4jLoader.class
			)
		)
		{
			var cfg = maker.getSpringContext ().getBean ( RdfDataManager.class ).getGeneralConfig ();
			maxStrLen = cfg.getMaxStringLength ();
			truncTrailer = cfg.getBigValueTrailer ();
		}
		
		NeoTestUtils.initNeo ();
		
		Rdf2PGCli.main ( 
			"--config", "target/test-classes/big-values/config.xml", 
			"--tdb", "target/rdf2neo-big-values-tdb",
			"target/test-classes/big-values/big-values.ttl"
		);
		
		// Find (p:Person) RETURN p.description
		// Check its len is <= maxStrLen
		// Check it ends with the truncation trailer
		try ( var driver = NeoTestUtils.getNeoDriver () )
		{
			var session = driver.session ();
			var cursor = session.run ( "MATCH (p:Person) RETURN p.description AS desc" );
			assertTrue ( "No Person node found!", cursor.hasNext () );
			var desc = cursor.next ().get ( "desc" ).asString ();
			assertTrue ( "Description not truncated!", desc.length () <= maxStrLen );
			assertTrue ( "Description doesn't end with the truncation trailer!", 
				desc.endsWith ( truncTrailer ) 
			);
		}
	}
	
	/**
	 * As per {@link GeneralConfig.BigValueMode#TRUNCATE}, a multi-value property is truncated to the configured
	 * max len, divided by the number of values. 
	 */
	@Test
	public void testBigMultiStringTruncation ()
	{
		int maxStrLen = 0;
		String truncTrailer = null;
		
		try (
			var maker = MultiConfigPGMaker.getSpringInstance (
				"target/test-classes/big-values/config.xml", MultiConfigNeo4jLoader.class
			)
		)
		{
			var cfg = maker.getSpringContext ().getBean ( RdfDataManager.class ).getGeneralConfig ();
			maxStrLen = cfg.getMaxStringLength ();
			truncTrailer = cfg.getBigValueTrailer ();
		}
		
		NeoTestUtils.initNeo ();
		
		Rdf2PGCli.main ( 
			"--config", "target/test-classes/big-values/config.xml", 
			"--tdb", "target/rdf2neo-big-values-tdb",
			"target/test-classes/big-values/big-values.ttl"
		);
		
		// Find (p:Person) RETURN p.notes, which is a list of strings
		// Check total len is <= maxStrLen
		// Check each value ends with the truncation trailer
		try ( var driver = NeoTestUtils.getNeoDriver () )
		{
			var session = driver.session ();
			var cursor = session.run ( "MATCH (p:Person) RETURN p.notes AS notes" );
			assertTrue ( "No Person node found!", cursor.hasNext () );
			var notes = cursor.next ().get ( "notes" ).asList ( v -> v.asString () );
			
			int totalLen = notes
				.stream ()
				.peek ( s -> System.out.println ( "Note: " + s ) )
				.mapToInt ( String::length )
				.sum ();
			assertTrue ( "Notes not truncated!", totalLen <= maxStrLen );
			
			for ( String note: notes )
			{
				assertTrue ( "Note doesn't end with the truncation trailer!", 
					note.endsWith ( truncTrailer ) 
				);
			}
		}		
	}
	
	/**
	 * Too big lists/sets yields errors when not {@link GeneralConfig.BigValueMode#IGNORE}.
	 * 
	 * TODO: actually the IllegalArgumentException it throws is intercepted and neutralised.
	 * Here we verify by testing the failing property is not loaded.
	 * 
	 */
	@Test
	public void testBigListThrowsError ()
	{
		NeoTestUtils.initNeo ();
		
		Rdf2PGCli.main ( 
			"--config", "target/test-classes/big-values/config.xml", 
			"--tdb", "target/rdf2neo-big-values-n-sets-tdb",
			"target/test-classes/big-values/big-values-n-sets.ttl"
		);
		
		try ( var driver = NeoTestUtils.getNeoDriver () )
		{
			var session = driver.session ();
			var cursor = session.run ( "MATCH (p:Person) RETURN p.friendIds AS friendIds" );
			assertFalse ( "Big passed loading!", cursor.hasNext () );
		}
		
	}
}

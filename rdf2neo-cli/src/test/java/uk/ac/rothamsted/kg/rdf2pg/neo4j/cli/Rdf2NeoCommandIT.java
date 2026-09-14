package uk.ac.rothamsted.kg.rdf2pg.neo4j.cli;

import static org.junit.Assert.assertEquals;

import org.junit.ClassRule;
import org.junit.Test;

import uk.ac.rothamsted.kg.rdf2pg.cli.Rdf2PGCli;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.test.NeoTestUtils;
import uk.ac.rothamsted.kg.rdf2pg.test.DataTestUtils;
import uk.ac.rothamsted.neo4j.utils.test.NeoTestContainerResource;

/**
 * The test for the Neo CLI
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>30 Nov 2020</dd></dl>
 *
 */
public class Rdf2NeoCommandIT
{
	@ClassRule
	public static NeoTestContainerResource neoContainer = new NeoTestContainerResource ();
		
	/**
	 * From an existing TDB
	 */
	@Test
	public void testTdb2Neo ()
	{
		DataTestUtils.initDBpediaDataSet ();
		
		Rdf2PGCli.main ( 
			"--config", "target/test-classes/test_config.xml",
			"--tdb", DataTestUtils.TDB_PATH
		);
		// TODO: test!
		assertEquals ( "Bad exit code!", 0, Rdf2PGCli.getExitCode () );
	}

	/**
	 * First populates a TDB, then invokes the conversion.
	 */
	@Test
	public void testRdf2Neo ()
	{
		NeoTestUtils.initNeo ( neoContainer.getNeoDriver () );
		
		var dbpath = "target/test-classes/examples/dbpedia/";
		
		Rdf2PGCli.main ( 
			"--config", "target/test-classes/test_config.xml", 
			"--tdb", "target/rdf2neo-test-tdb",
			dbpath + "dbpedia_places.ttl",
			dbpath + "dbpedia_people.ttl"
		);
		// TODO: test!
		assertEquals ( "Bad exit code!", 0, Rdf2PGCli.getExitCode () );
	}

}

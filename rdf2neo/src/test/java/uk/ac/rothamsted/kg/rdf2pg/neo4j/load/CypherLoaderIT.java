package uk.ac.rothamsted.kg.rdf2pg.neo4j.load;

import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testcontainers.neo4j.Neo4jContainer;

import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.spring.SimpleCyLoaderFactory;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.CyNodeLoadingHandler;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.CyNodeLoadingProcessor;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.CyRelationLoadingHandler;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.CyRelationLoadingProcessor;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.Neo4jDataManager;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.test.NeoTestUtils;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.ConfigItem;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.MultiConfigPGMaker;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.PropertyGraphMaker;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.rdf.RdfDataManager;
import uk.ac.rothamsted.kg.rdf2pg.test.DataTestUtils;
import uk.ac.rothamsted.neo4j.utils.test.CypherTester;
import uk.ac.rothamsted.neo4j.utils.test.NeoTestContainerResource;

/**
 * Basic tests for {@link SimpleCyLoader} and {@link MultiConfigPGMaker}.
 *
 * As developer user, you're probably interested in invoking the converter using Spring configuration,
 * @see {@link #testSpringMultiConfig()} or {@link #testNeoIndexing()}. 
 *   
 * @author brandizi
 * <dl><dt>Date:</dt><dd>14 Dec 2017</dd></dl>
 *
 */
public class CypherLoaderIT
{
	@ClassRule
	public static NeoTestContainerResource neoContainer = new NeoTestContainerResource ();
	
	private Logger log = org.slf4j.LoggerFactory.getLogger ( this.getClass () );

	
	@BeforeClass
	public static void initTDB ()
	{
		DataTestUtils.initDBpediaDataSet ();
	}
	
	
	@Before
	public void initNeoData () {		
		NeoTestUtils.initNeo ( neoContainer.getNeoDriver () );
	}
		

	@Test
	public void testLoading ()
	{
		try (
			var cyloader = new SimpleCyLoader ();
			var rdfMgr = new RdfDataManager ( DataTestUtils.TDB_PATH );
		)
		{
			// You don't want to do this, see #testSpring()
			
			Neo4jDataManager neoMgr = new Neo4jDataManager ( neoContainer.getNeoDriver () );
			
			CyNodeLoadingHandler cyNodeHandler = new CyNodeLoadingHandler ();
			CyRelationLoadingHandler cyRelHandler = new CyRelationLoadingHandler ();
			
			cyNodeHandler.setLabelsSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_LABELS );
			cyNodeHandler.setNodePropsSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_PROPS );
			cyNodeHandler.setRdfDataManager ( rdfMgr );
			cyNodeHandler.setNeo4jDataManager ( neoMgr );
			
			cyRelHandler.setRelationTypesSparql ( DataTestUtils.DBPEDIA_SPARQL_REL_TYPES );
			cyRelHandler.setRelationPropsSparql ( DataTestUtils.DBPEDIA_SPARQL_REL_PROPS );
			cyRelHandler.setRdfDataManager ( rdfMgr );
			cyRelHandler.setNeo4jDataManager ( neoMgr );

			CyNodeLoadingProcessor cyNodeProc = new  CyNodeLoadingProcessor();
			cyNodeProc.setNodeIrisSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_IRIS );
			cyNodeProc.setBatchJob ( cyNodeHandler );
			
			CyRelationLoadingProcessor cyRelProc = new CyRelationLoadingProcessor ();
			cyRelProc.setConsumer ( cyRelHandler );

			cyloader.setPGNodeMaker ( cyNodeProc );
			cyloader.setPGRelationMaker ( cyRelProc );
			
			cyloader.make ( DataTestUtils.TDB_PATH );
			// TODO: test!
			
		} // try neoDriver
	}
	

	@Test
	public void testMultiConfigLoading ()
	{
		try ( var cymloader = new MultiConfigNeo4jLoader (); )
		{
			// You don't want to do this, see #testSpring()
			cymloader.setPGMakerFactory ( 
				new SimpleCyLoaderFactory () 
				{
					@Override
					public SimpleCyLoader getObject () throws BeansException
					{
						RdfDataManager rdfMgr = new RdfDataManager ();
						Neo4jDataManager neoMgr = new Neo4jDataManager ( neoContainer.getNeoDriver () );			
						
						CyNodeLoadingHandler cyNodeHandler = new CyNodeLoadingHandler ();
						CyRelationLoadingHandler cyRelHandler = new CyRelationLoadingHandler ();
						
						cyNodeHandler.setRdfDataManager ( rdfMgr );
						cyNodeHandler.setNeo4jDataManager ( neoMgr );
						
						cyRelHandler.setRdfDataManager ( rdfMgr );
						cyRelHandler.setNeo4jDataManager ( neoMgr );
			
						CyNodeLoadingProcessor cyNodeProc = new CyNodeLoadingProcessor ();
						cyNodeProc.setBatchJob ( cyNodeHandler );
						
						CyRelationLoadingProcessor cyRelProc = new CyRelationLoadingProcessor ();
						cyRelProc.setConsumer ( cyRelHandler );
			
						SimpleCyLoader cyloader = new SimpleCyLoader ();
						cyloader.setPGNodeMaker ( cyNodeProc );
						cyloader.setPGRelationMaker ( cyRelProc );
						cyloader.setRdfDataManager ( rdfMgr );
						
						return cyloader;
					} // getObject()
				} // SimpleCyloaderFactory
			); // setPGMakerFactory()
	
			
			List<ConfigItem<SimpleCyLoader>> config = new LinkedList<> ();
			{
				var cfgi = new ConfigItem<SimpleCyLoader> ();
				cfgi.setName ( "places" );
				cfgi.setNodeIrisSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_IRIS );
				cfgi.setLabelsSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_LABELS );
				cfgi.setNodePropsSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_PROPS );
				cfgi.setRelationTypesSparql ( DataTestUtils.DBPEDIA_SPARQL_REL_TYPES );
				cfgi.setRelationPropsSparql ( DataTestUtils.DBPEDIA_SPARQL_REL_PROPS );
				config.add ( cfgi );
			}

			{
				var cfgi = new ConfigItem<SimpleCyLoader> ();
				cfgi.setName ( "people" );

				cfgi.setNodeIrisSparql ( DataTestUtils.DBPEDIA_SPARQL_PEOPLE_IRIS );
				cfgi.setLabelsSparql ( DataTestUtils.DBPEDIA_SPARQL_PEOPLE_LABELS );
				cfgi.setNodePropsSparql ( DataTestUtils.DBPEDIA_SPARQL_PEOPLE_PROPS );
				cfgi.setRelationTypesSparql ( DataTestUtils.DBPEDIA_SPARQL_PEOPLE_REL_TYPES );
				config.add ( cfgi );
			}
			
			cymloader.setConfigItems ( config );
	
			cymloader.load ( DataTestUtils.TDB_PATH );
		}
		// TODO: test!
	}	
	
	
	@Test
	public void testSpring ()
	{
		// Use ConfigurableApplicationContext to show the try() block that it's a Closeable and let Java to clean up
	  // automatically 
		try ( ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "test_config.xml" ); )
		{			
			PropertyGraphMaker cyloader = beanCtx.getBean ( SimpleCyLoader.class );
			cyloader.make ( DataTestUtils.TDB_PATH );
			// TODO: test
		}
	}	

	
	@Test
	public void testSpringMultiConfig ()
	{
		try ( 
			ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "multi_config.xml" );
			MultiConfigNeo4jLoader mloader = MultiConfigNeo4jLoader.getSpringInstance ( beanCtx, MultiConfigNeo4jLoader.class );				
		)
		{			
			mloader.load ( DataTestUtils.TDB_PATH );
			var cytest = new CypherTester ( beanCtx.getBean ( Driver.class ) );
			assertTrue ( "Player data not found!", cytest.ask ( 
				"MATCH (player:Person{label:'Atsuto Tatara'}) - [hasTeam:team] -> (team:SportsTeam)\n" +
				"RETURN COUNT(DISTINCT team) > 2"
			));
			assertTrue ( "Added relation constant attributes not found!", cytest.ask ( 
				"MATCH ()-[r{relationProvenance:'examples'}]->()\n" +
				"RETURN COUNT(DISTINCT r) > 0"
			));
		}
	}	

	
	@Test
	public void testNeoIndexing ()
	{		
		try ( 
			ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "multi_config_indexing.xml" );
			MultiConfigNeo4jLoader mloader = MultiConfigNeo4jLoader.getSpringInstance ( beanCtx, MultiConfigNeo4jLoader.class );				
		)
		{			
			mloader.load ( DataTestUtils.TDB_PATH );
			// TODO: CALL db.indexes(); to test for the index existence.
		}
	}		
}

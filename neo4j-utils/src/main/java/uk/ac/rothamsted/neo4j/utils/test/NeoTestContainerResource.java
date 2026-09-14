package uk.ac.rothamsted.neo4j.utils.test;

import org.junit.rules.ExternalResource;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.testcontainers.neo4j.Neo4jContainer;

/**
 * A simple helper to setup Neo4j from test containers and get a Driver instance linked to it.
 * 
 * This is a JUnit {@link ExternalResource} rule.
 * 
 * <b>WARNING</b>: Junit and test container dependencies are declared in this utility ad optional, 
 * because we don't want test stuff to reach your distro bins. You'll need to add these dependencies
 * with test scope in your project to use this utility.
 * 
 * TODO: migrate to <a href = "https://www.baeldung.com/junit-5-extensions">JUnit 5 extensions</a>.
 *
 * @author Marco Brandizi
 * <dl><dt>Date:</dt><dd>14 Sept 2026</dd></dl>
 *
 */
public class NeoTestContainerResource extends ExternalResource
{
	/**
	 * Set this to establish the Neo4j version to be pulled by the test container
	 */
	public static final String PROP_NEO_SERVER_VERSION = "neo4j.server.version";
	
	/**
	 * This is set upon {@link #before() container initialisation}, to make it available to your 
	 * code, eg, Spring configurations.
	 */
	public static final String PROP_NEO_CLI_URI = "neo4j.client.uri";
	
	private Driver neoDriver;
	private Neo4jContainer neoContainer;
	
	/**
	 * TODO: allow for a container factory, which could be used to configure the container and alike.
	 * 
	 */
	@Override
	@SuppressWarnings ( "resource" )
	protected void before ()
	{
		String neoVersion = System.getProperty ( PROP_NEO_SERVER_VERSION );
		
		neoContainer = new Neo4jContainer ( "neo4j:" + neoVersion )
		.withoutAuthentication ();
		
		neoContainer.start ();
		
		neoDriver = GraphDatabase.driver ( neoContainer.getBoltUrl () );
		
		// Inject these into the Spring config XMLs
		System.setProperty ( PROP_NEO_CLI_URI, neoContainer.getBoltUrl () );		
		
	}

	@Override
	protected void after ()
	{
		if ( neoDriver != null ) neoDriver.close ();
		if ( neoContainer != null ) neoContainer.stop ();
	}
	
	public Driver getNeoDriver ()
	{
		return neoDriver;
	}
	
	/**
	 * The test container instance. Might be useful to get properties like the Bolt URL. 
	 */
	public Neo4jContainer getNeoContainer ()
	{
		return neoContainer;
	}
}

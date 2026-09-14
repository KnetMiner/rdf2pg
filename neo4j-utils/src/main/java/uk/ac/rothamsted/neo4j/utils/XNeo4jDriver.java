package uk.ac.rothamsted.neo4j.utils;

import java.util.concurrent.CompletionStage;

import org.apache.commons.lang3.ObjectUtils;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.BaseSession;
import org.neo4j.driver.BookmarkManager;
import org.neo4j.driver.Driver;
import org.neo4j.driver.ExecutableQuery;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

/**
 * 
 * The Neo4j Driver extension
 * 
 * An extended version of the {@link Driver Neo4j driver class}, which has 
 * some utilities, such as keeping a default DB name and using it with a 
 * {@link #defaultSessionConfig()}.
 * 
 * TODO: write tests.
 *
 * @author Marco Brandizi
 * <dl><dt>Date:</dt><dd>13 Mar 2024</dd></dl>
 *
 */
public class XNeo4jDriver implements Driver
{
	private Driver driver;
	private String databaseName;
	private SessionConfig defaultSessionConfig;
		
	public XNeo4jDriver ( Driver driver, String databaseName )
	{
		super ();
		this.driver = driver;
		this.databaseName = databaseName;
		this.defaultSessionConfig = sessionConfigBuilder ().build ();
	}
	
	/**
	 * When database name is null, defaults to the usual default session config.
	 * 
	 * @see #defaultSessionConfig()
	 * 
	 */
	public XNeo4jDriver ( Driver driver ) {
		this ( driver, null );
	}

	/**
	 * A default config that sets the database name to the one passed via constructor.
	 * 
	 * All session-creation methods that don't have a config parameter are invoked
	 * with this default, eg, {@link #session()}, {@link #session(Class)} create
	 * sessions that use this.
	 * 
	 * If the database name is null, this defaults to {@link SessionConfig#defaultConfig()}.
	 * 
	 * Note that this session config is cached, so, if you don't need a custom session config,
	 * this is a bit faster than {@link #sessionConfigBuilder()}.
	 * 
	 * The original database name can be fetched via {@link SessionConfig#database()}.
	 * 
	 * 
	 */
	public SessionConfig defaultSessionConfig ()
	{
		return defaultSessionConfig;
	}
	
	/**
	 * An initial session builder which embeds the database name.
	 * 
	 * This can be used as an alternative to {@link #defaultSessionConfig()}, 
	 * to build a custom session with the parameters you wish.
	 * 
	 * Note that {@link #defaultSessionConfig()} is cached, so, if you don't need
	 * a special session, use that method.  
	 */
	public SessionConfig.Builder sessionConfigBuilder ()
	{
		SessionConfig.Builder builder = SessionConfig.builder ();
		if ( !ObjectUtils.isEmpty ( databaseName ) ) 
			builder = builder.withDatabase ( databaseName );
		return builder;
	}
	
	
	@Override
	public ExecutableQuery executableQuery ( String query )
	{
		return driver.executableQuery ( query );
	}

	@Override	
	public BookmarkManager executableQueryBookmarkManager ()
	{
		return driver.executableQueryBookmarkManager ();
	}

	@Override	
	public boolean isEncrypted ()
	{
		return driver.isEncrypted ();
	}

	@Override	
	public Session session ()
	{
		return driver.session ( defaultSessionConfig () );
	}

	/**
	 * WARNING: if you create a session with {@link SessionConfig#builder()}, 
	 * this <b>will not</b> use the default database name set for this driver.
	 * Use {@link #sessionConfigBuilder()} to create a session config that does that.
	 * 
	 * TODO: if a 3rd-party method gets Driver and creates a session config, it 
	 * can't work as said above. A possible solution is that this method creates 
	 * {@link #sessionConfigBuilder() its own builder} and then copies the 3rd-party 
	 * config into it. However, we would need to start caching session configs.
	 */
	@Override	
	public Session session ( SessionConfig sessionConfig )
	{
		return driver.session ( sessionConfig );
	}

	@Override
	public <T extends BaseSession> T session ( Class<T> sessionClass )
	{
		return driver.session ( sessionClass, defaultSessionConfig () );
	}

	@Override
	public <T extends BaseSession> T session ( Class<T> sessionClass, AuthToken sessionAuthToken )
	{
		return driver.session ( sessionClass, defaultSessionConfig (), sessionAuthToken );
	}

	@Override
	public <T extends BaseSession> T session ( Class<T> sessionClass, SessionConfig sessionConfig )
	{
		return driver.session ( sessionClass, sessionConfig );
	}

	@Override
	public <T extends BaseSession> T session ( 
		Class<T> sessionClass, SessionConfig sessionConfig, AuthToken sessionAuthToken )
	{
		return driver.session ( sessionClass, sessionConfig, sessionAuthToken );
	}

	@Override
	public void close ()
	{
		driver.close ();
	}

	@Override
	public CompletionStage<Void> closeAsync ()
	{
		return driver.closeAsync ();
	}

	@Override
	public void verifyConnectivity ()
	{
		driver.verifyConnectivity ();
	}

	@Override
	public CompletionStage<Void> verifyConnectivityAsync ()
	{
		return driver.verifyConnectivityAsync ();
	}

	@Override
	public boolean verifyAuthentication ( AuthToken authToken )
	{
		return driver.verifyAuthentication ( authToken );
	}

	@Override
	public boolean supportsSessionAuth ()
	{
		return driver.supportsSessionAuth ();
	}

	@Override
	public boolean supportsMultiDb ()
	{
		return driver.supportsMultiDb ();
	}

	@Override
	public CompletionStage<Boolean> supportsMultiDbAsync ()
	{
		return driver.supportsMultiDbAsync ();
	}

}

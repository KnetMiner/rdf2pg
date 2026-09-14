package uk.ac.rothamsted.kg.rdf2pg.pgmaker;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import uk.ac.ebi.utils.collections.OptionsMap;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.spring.SimplePGMakerFactory;


/**
 * <H1>The multi-configuration property graph generator.</H1>
 *
 * <p>This uses multiple {@link ConfigItem SPARQL query configurations} to run {@link SimplePGMaker} multiple times.
 * This allows for logically separated items in an RDF data set to be mapped separately, each with its own set of SPARQL
 * queries.</p> 
 *
 * <p>Note also that everything is designed to support configuration via Spring Bean files.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Jan 2018</dd></dl>
 *
 * Modified by cbobed for refactoring purposes  
 * <dl><dt>Date:</dt><dd>28 Apr 2020</dd></dl>
 */
public abstract class MultiConfigPGMaker<CI extends ConfigItem<SM>, SM extends SimplePGMaker<?, ?, ?, ?>>
  implements PropertyGraphMaker, AutoCloseable
{
	private List<CI> configItems = new LinkedList<> ();
	private SimplePGMakerFactory<SM> pgMakerFactory;
	
	private ApplicationContext springContext;
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	private static Logger slog = LoggerFactory.getLogger ( MultiConfigPGMaker.class );

	
	/** 
	 * Gets an instance from the Spring application context. The returned instance is bound to the beanCtx parameter,
	 * so that {@link MultiConfigPGMaker#close()} can close it.
	 * 
	 * See the XML examples to know how you should configure
	 * beans for this.
	 * 
	 */
	public static <MM extends MultiConfigPGMaker<?, ?>> MM getSpringInstance ( 
		ApplicationContext beanCtx, Class<? extends MM> makerClass
	)
	{
		slog.info ( "Getting maker configuration from Spring Context" );
		MM multiMaker = beanCtx.getBean ( makerClass );
		multiMaker.setSpringContext ( beanCtx );
		return multiMaker;
	}

	/**
	 * Invokes {@link #getSpringInstance(ApplicationContext)} with the context obtained from the XML 
	 * configuration file.
	 * 
	 * The instance returned this way will close the application context when the {@link MultiConfigPGMaker#close()}
	 * method is invoked.
	 *  
	 */
	public static <MM extends MultiConfigPGMaker<?, ?>> MM getSpringInstance ( String xmlConfigPath, Class<? extends MM> makerClass )
	{
		slog.info ( "Getting maker configuration from Spring XML file '{}'", xmlConfigPath );		
		ApplicationContext ctx = new FileSystemXmlApplicationContext ( xmlConfigPath );
		return getSpringInstance ( ctx, makerClass );
	}
	
	
	/**
	 * The setter is protected, since only {@link #getSpringInstance(ApplicationContext)} 
	 * needs that. This getter is public instead, for tests.
	 * 
	 */
	public ApplicationContext getSpringContext ()
	{
		return springContext;
	}

	protected void setSpringContext ( ApplicationContext springContext )
	{
		this.springContext = springContext;
	}

	/**
	 * Does the job in a multi-configuration mode.
	 * 
	 * Namely, 
	 * 
	 * <p>First, runs {@link #makeBegin(String, OptionsMap)}</p>
	 * 
	 * <p>Then, for each of {@link SimplePGMaker} makeXXX methods 
	 * (eg, {@link SimplePGMaker#makeNodes(String, OptionsMap)}, loops through all the
	 * {@link #getConfigItems() configured items} and executes the respective configuration
	 * (eg, node processing with the config's node-related SPARQL). This happens thanks to
	 * {@link #makeStage(Consumer, ConfigItem, String)}.</p>
	 * 
	 * <p>Finally, runs {@link #makeEnd(String, OptionsMap)}.</p>
	 * 
	 */
	@Override
	public void make ( String tdbPath, OptionsMap opts )
	{
		this.makeBegin ( tdbPath, opts );
		
		// First the nodes and then the relations
		// That ensures that cross-references made by different queries are taken
		
		Stream<Consumer<SM>> stages = Stream.of ( 
			sm -> sm.makeNodes ( tdbPath, opts ),
			sm -> sm.makeRelations ( tdbPath, opts ),
			sm -> sm.makeIndexes ( tdbPath, opts )
		);

		stages.forEach ( stage -> {
			for ( CI cfg: this.getConfigItems () )
				this.makeStage ( stage, cfg, tdbPath );
		});
		
		this.makeEnd ( tdbPath, opts );
	}
	
	/**
	 * This default just logs that it's beginning with the current maker.
	 * 
	 * @see #make(String, OptionsMap)
	 */
	protected void makeBegin ( String tdbPath, OptionsMap opts )
	{
		log.info ( "Using {} PG maker", this.getClass ().getSimpleName () );
	}
	
	/**
	 * @see #make(String, OptionsMap)
	 * 
	 * This uses {@link #getPGMakerFactory()} to run the current stage over a single config
	 * maker.
	 */
	protected void makeStage ( Consumer<SM> stage, CI cfg, String tdbPath )
	{
		try ( SM pgSimpleMaker = this.getPGMakerFactory ().getObject (); )
		{
			cfg.configureMaker ( pgSimpleMaker );
			stage.accept ( pgSimpleMaker );
		}
	}
	
	/**
	 * The default doesn't do anything.
	 * 
	 * @see #make(String, OptionsMap)
	 */
	protected void makeEnd ( String tdbPath, OptionsMap opts )
	{
		// Defaults to null
	}
	
	
	
	/**
	 * A single configuration defines how nodes and relations are mapped from RDF.
	 * 
	 * @see {@link #make(String, Object...)} 
	 */
	public List<CI> getConfigItems ()
	{
		return configItems;
	}

	
	@Autowired ( required = false )
	public void setConfigItems ( List<CI> configItems )
	{
		this.configItems = configItems;
	}

	/**
	 * This is used in {@link #makeStage(Consumer, ConfigItem, String)}, to get a new {@link SimplePGMaker} 
	 * to be used with a new configuration while iterating over {@link #getConfigItems()}. 
	 * 
	 * This is designed this way in order to make it possible to configure/autowire
	 * a factory via Spring.
	 * 
	 */
	public SimplePGMakerFactory<SM> getPGMakerFactory ()
	{
		return pgMakerFactory;
	}

	/**
	 * You need to create a bean of type {@link SimplePGMakerFactory} in the specific PG package.
	 * 
	 * @Resource creates problems with Java 11 (<a href = "https://stackoverflow.com/questions/56855335">see here</a>).
	 */
	@Autowired
	public void setPGMakerFactory ( SimplePGMakerFactory<SM> makerFactory )
	{
		this.pgMakerFactory = makerFactory;
	}
	
	
	/**
	 * This does something effectively if the current maker instance was obtained via one of the 
	 * {@link #getSpringInstance(ApplicationContext)} methods, namely, the corresponding Spring context is closed.
	 * 
	 * If the maker was obtained some other way, this method has no effect and you can safely call it, just in case.
	 * 
	 */
	@Override
	public void close ()
	{
		if ( this.springContext == null || ! ( springContext instanceof ConfigurableApplicationContext ) ) return;
		
		ConfigurableApplicationContext cfgCtx = (ConfigurableApplicationContext) springContext;
		cfgCtx.close ();
	}

}

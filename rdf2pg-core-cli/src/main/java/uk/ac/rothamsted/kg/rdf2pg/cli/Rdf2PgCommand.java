package uk.ac.rothamsted.kg.rdf2pg.cli;

import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.tdb2.loader.Loader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.GeneralConfig;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.MultiConfigPGMaker;

/**
 * The common skeleton for a command that converts from RDF files to a property graph, or from
 * a TDB to a PG (when no RDF is specified).
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>1 Dec 2020</dd></dl>
 *
 */
public abstract class Rdf2PgCommand<MM extends MultiConfigPGMaker<?, ?>> extends CliCommand
{
	@Option ( 
		names = { "-c", "--config" }, 
		description = "Configuration file (see examples/). "
			+ "WARNING! use 'file:///...' to specify absolute paths (Spring requirement).",
    required = true
	)
	protected String xmlConfigPath = "";

	@Option ( 
		names = { "-t", "--tdb" }, 
		paramLabel = "<path>", 
		description = 
			"The path to the Jena TDB triple store to load from. It's created if doesn't exist, "
		  + " the existing content is taken if it exists.",
		required = false,
		showDefaultValue = Visibility.ALWAYS
	)
	protected String tdbPath = "/tmp/rdf2pg-tdb";
	
	@Parameters (
		paramLabel = "<file>", 
		description = "RDF files to be uses as input. They're first uploaded on the support TDB."
			+ " If none, the --tdb store is taken with its current contents."
		
	)
	protected String [] rdfFilePaths;
	 
	
	@Option ( 
		names = { "-l", "--rdf-load" }, 
		description = "Only loads RDF files to the working TDB. This is save you from downloading Jena when you want to "
			+ "populate a TDB separately (eg, in data pipelines).",
		required = false,
		showDefaultValue = Visibility.ALWAYS
	)
	protected boolean rdfLoadOnly = false;
	
	
	/** 
	 * The implementors should pass this in the constructor and the {@link #getMakerFromSpringConfig()} will return 
	 * the specific maker to work with.  
	 */
	protected final Class<MM> makerClass;
	
	/**
	 * Injected here to allow {@link #call()} to log its values.
	 */
	@Autowired ( required = false ) @Qualifier ( "generalConfig" )
	protected GeneralConfig generalConfig;
	
	
	protected Rdf2PgCommand ( Class<MM> makerClass )
	{
		this.makerClass = makerClass;
	}
	
	/**
	 * Helper that calls {@link MultiConfigPGMaker#getSpringInstance(String, Class)} using {@link #xmlConfigPath}
	 * as file parameter and using {@link #makerClass} for the maker to be fetched.
	 * 
	 */
	protected MM getMakerFromSpringConfig ()
	{
		return MultiConfigPGMaker.getSpringInstance ( xmlConfigPath, makerClass );
	}

	
	/**
	 * This is a skeleton of how we want an Rdf2Pg command to work, that's why it's final. 
	 * Here, we check if we have {@link #rdfFilePaths} to load into the {@link #tdbPath support TDB}, possibly 
	 * {@link #load2Tdb() we do it} and then we invoke the {@link #makePropertyGraph() actual PG conversion from TDB}.
	 * 
	 * So, implement {@link #makePropertyGraph()} for your specific converter.
	 * 
	 */
	@Override
	public final Integer call () throws Exception
	{
		log.info ( "General configuration loaded: {}", generalConfig );
		if ( this.rdfFilePaths != null && rdfFilePaths.length > 0 ) this.load2Tdb ();
		return rdfLoadOnly ? 0 : this.makePropertyGraph ();
	}


	public abstract int makePropertyGraph () throws Exception;

	/**
	 * This populates {@link #tdbPath} with the files specified via the #rdfFilePaths option.
	 */
	protected void load2Tdb ()
	{
		log.info ( "Starting TDB Loading" );
		var dataset = TDB2Factory.connectDataset ( this.tdbPath );
		Loader.load ( dataset.asDatasetGraph (), true, this.rdfFilePaths );
		log.info ( "TDB Loading ended" );
	}
		
}

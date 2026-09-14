package uk.ac.rothamsted.kg.rdf2pg.pgmaker.support;

import static info.marcobrandizi.rdfutils.jena.JenaGraphUtils.JENAUTILS;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import info.marcobrandizi.rdfutils.jena.TDBEndPointHelper;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.ConfigItem;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.DefaultNamedPGMakeComp;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.spring.PGMakerSessionScope;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.rdf.RdfDataManager;

/**
 * The indexer component.
 * 
 * In general, this takes the index definitions coming from {@link #getIndexesSparql()}, 
 * pre-processes them (eg, normalises IDs using {@link RdfDataManager#getPGNodeLabelIdConverter()},
 * and then passes them to {@link #index(List)}, the method that does the real/specific indexing 
 * job. What the latter does depends on what you need on the PG target, eg, you might issue
 * Cypher commands.
 * 
 * We use {@link SimpleTsvIndexer} as default. See the documentation and the example config
 * files.
 * 
 * <b>WARNING</b>: you need to repeat this in the subclasses: 
 * {@code @Component @Primary @Scope ( scopeName = "pgmakerSession" )}. @Component and
 * @Scope aren't inherited, while @Primary cause Spring to override the default indexer
 * that is defined in the hereby Maven core module, which is {@link SimpleTsvIndexer}.
 * 
 * You can always disable the indexing by omitting 
 * {@link ConfigItem#getIndexesSparql() the indexes SPARQL}.
 * 
 * Also, you might want the {@link PGMakerSessionScope pgmakerSession scope}, to allow it
 * proper re-initialisation when a new {@link ConfigItem} is processed.
 * 
 * <b>WARNING</b>: we don't add the 'iri' property by default, since we can't know if the
 * target needs it or not. It's up to the specific implementation of the indexer to possibly
 * ass such property (for both nodes and relations), or, alternatively, it's up to the target
 * PG node/relation makers to automatically index on iri.
 * 
 * @author brandizi
 * <dl><dt>Date:</dt><dd>31 May 2023</dd></dl> (abstracted from the existing Cypher indexer)
 * 
 * TODO: update the documentation.
 */
@Component
public abstract class PGIndexer extends DefaultNamedPGMakeComp
{
	/**
	 * The data extracted and normalised from {@link PGIndexer#getIndexesSparql()}.
	 */
	public static class IndexDef
	{
		private String type;
		private String propertyName;
		private boolean isRelation;
		
		private IndexDef ( String type, String propertyName, boolean isRelation )
		{
			super ();
			this.type = type;
			this.propertyName = propertyName;
			this.isRelation = isRelation;
		}

		public String getType ()
		{
			return type;
		}

		public String getPropertyName ()
		{
			return propertyName;
		}

		public boolean isRelation ()
		{
			return isRelation;
		}
	}
	
	private RdfDataManager rdfDataManager;
	
	private String indexesSparql;

	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () ); 
	
	
	protected abstract void index ( List<IndexDef> indexDefinitions );
	

	public void index ()
	{
		var indexDefs = this.loadIndexDefinitions ();
		index ( indexDefs );
	}
	
		
	private List<IndexDef> loadIndexDefinitions ()
	{
		String idxSparql = getIndexesSparql ();
		if ( idxSparql == null ) return List.of();

		log.info ( "{}Loading Index definitions", getCompNamePrefix () );

		RdfDataManager rdfMgr = this.getRdfDataManager ();
		
		Function<String, String> labelIdConverter = rdfMgr.getPGNodeLabelIdConverter ();
		Function<String, String> propIdConverter = rdfMgr.getPGPropertyIdConverter ();
				
		List<IndexDef> result = new ArrayList<> ();
		
		rdfMgr.processSelect ( "PGIndexer", idxSparql, row -> 
		{  
			String label = rdfMgr.getPGId ( row.get ( "label" ), labelIdConverter );
			if ( label == null ) throw new IllegalArgumentException ( "Null label in the indices query" );
			
			// id converter has to be possibly used later, not for "all nodes|labels"
			String propName = rdfMgr.getPGId ( row.get ( "propertyName" ), null );
			if ( propName == null ) throw new IllegalArgumentException ( String.format (  
				"Null property name in the indices query, for the label %s", label 
			));
			
			propName = propIdConverter.apply ( propName );
			
			var isRelation = JENAUTILS.literal2Boolean ( row.getLiteral ( "isRelation" ) ).orElse ( false );
					
			result.add ( new IndexDef ( label, propName, isRelation ) );

		}); // processSelect()

		log.info ( "{}Index definitions loaded, returning {} row(s)", getCompNamePrefix (), result.size () );
		
		return result;
	} // loadIndexDefinitions ()

	
	/**
	 * Used internally, to interact with an RDF backend. We don't care about 
	 * {@link TDBEndPointHelper#close() closing} it, so the caller has to do it.
	 */
	public RdfDataManager getRdfDataManager ()
	{
		return rdfDataManager;
	}

	@Autowired
	public void setRdfDataManager ( RdfDataManager rdfDataManager )
	{
		this.rdfDataManager = rdfDataManager;
	}

	/**
	 * See above.
	 */
	public String getIndexesSparql ()
	{
		return indexesSparql;
	}

	public void setIndexesSparql ( String indexesSparql )
	{
		this.indexesSparql = indexesSparql;
	}
}

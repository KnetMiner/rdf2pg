package uk.ac.rothamsted.kg.rdf2pg.pgmaker;

import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGIndexer;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGNodeHandler;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGNodeMakeProcessor;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGRelationHandler;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGRelationMakeProcessor;

/**
 * Represents a configuration to be used for the conversion of a single node or relation type
 * to a property graph.
 * 
 * @author brandizi
 * <dl><dt>Date:</dt><dd>20 Jan 2018</dd></dl>
 * 
 */
public class ConfigItem<SM extends SimplePGMaker<?,?,?,?>>
{
	private String name;
	
	private String nodeIrisSparql, labelsSparql, nodePropsSparql;
	private String relationTypesSparql, relationPropsSparql;
	
	private String indexesSparql;

	
	public ConfigItem () {}

		
	/**
	 * @see {@link SimplePGMaker#getComponentName()}.
	 */
	public String getName () {
		return name;
	}
	public void setName ( String name ) {
		this.name = name;
	}
	
	/**
	 * @see {@link PGNodeMakeProcessor#getNodeIrisSparql()}. 
	 */
	public String getNodeIrisSparql () {
		return nodeIrisSparql;
	}
	public void setNodeIrisSparql ( String nodeIrisSparql ) {
		this.nodeIrisSparql = nodeIrisSparql;
	}

	/**
	 * @see PGNodeHandler#getLabelsSparql()
	 */
	public String getLabelsSparql () {
		return labelsSparql;
	}
	public void setLabelsSparql ( String labelsSparql ) {
		this.labelsSparql = labelsSparql;
	}
	
	/**
	 * @see PGNodeHandler#getNodePropsSparql()
	 */
	public String getNodePropsSparql () {
		return nodePropsSparql;
	}
	public void setNodePropsSparql ( String nodePropsSparql ) {
		this.nodePropsSparql = nodePropsSparql;
	}
	
	/**
	 * @see {@link PGRelationHandler#getRelationTypesSparql()}. 
	 */
	public String getRelationTypesSparql () {
		return relationTypesSparql;
	}
	public void setRelationTypesSparql ( String relationTypesSparql ) {
		this.relationTypesSparql = relationTypesSparql;
	}
	
	/**
	 * @see {@link PGRelationHandler#getRelationPropsSparql()}. 
	 */
	public String getRelationPropsSparql () {
		return relationPropsSparql;
	}
	public void setRelationPropsSparql ( String relationPropsSparql ) {
		this.relationPropsSparql = relationPropsSparql;
	}
	
	/**
	 * @see PGIndexer#getIndexesSparql() 
	 */
	public String getIndexesSparql ()
	{
		return indexesSparql;
	}

	public void setIndexesSparql ( String indexesSparql )
	{
		this.indexesSparql = indexesSparql;
	}	

	public void configureMaker ( SM simpleMaker )
	{
		simpleMaker.setComponentName ( this.getName () );
		
		PGNodeMakeProcessor<?> nodeMaker = simpleMaker.getPGNodeMaker ();
		PGRelationMakeProcessor<?> relMaker = simpleMaker.getPGRelationMaker ();
		
		PGNodeHandler nodeHandler = nodeMaker.getBatchJob ();
		PGRelationHandler relHandler = relMaker.getBatchJob ();
		
		nodeMaker.setNodeIrisSparql ( this.getNodeIrisSparql () );

		nodeHandler.setLabelsSparql ( this.getLabelsSparql () );
		nodeHandler.setNodePropsSparql ( this.getNodePropsSparql () );
		
		relHandler.setRelationTypesSparql ( this.getRelationTypesSparql () );
		relHandler.setRelationPropsSparql ( this.getRelationPropsSparql () );
		
		// if ( this.indexesSparql == null ) return;
		var indexer = simpleMaker.getPgIndexer (); 
		if ( indexer == null )
		{
			if ( this.indexesSparql != null ) throw new UnsupportedOperationException ( String.format (
				"%san index SPARQL query was specified in the configuration, but there is no indexing "
				+ "component for this PG target", simpleMaker.getCompNamePrefix () ));
		}
		else
			indexer.setIndexesSparql ( indexesSparql ); // which might go null
	}
}

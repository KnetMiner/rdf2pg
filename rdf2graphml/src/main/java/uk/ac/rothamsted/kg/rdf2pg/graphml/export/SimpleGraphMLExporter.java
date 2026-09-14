package uk.ac.rothamsted.kg.rdf2pg.graphml.export;

import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.collections.OptionsMap;
import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLNodeExportHandler;
import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLNodeExportProcessor;
import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLRelationExportHandler;
import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLRelationExportProcessor;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.SimplePGMaker;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.SimpleTsvIndexer;

/**
 * <h1>The Simple GraphML Exporter</h1>
 * 
 * This is just a wrapper of {@link SimplePGMaker} which wires together the right generics and defines the
 * right Spring annotations.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>26 Oct 2020</dd></dl>
 *
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public class SimpleGraphMLExporter
  extends SimplePGMaker <GraphMLNodeExportHandler, GraphMLRelationExportHandler, 
  												GraphMLNodeExportProcessor, GraphMLRelationExportProcessor>
{

	/**
	 * If the {@link #getPgIndexer()} is {@link SimpleTsvIndexer}, 
	 * and "graphmlOutPath" is available, then uses the latter
	 * to set up {@link SimpleTsvIndexer#getOutputPath()}.
	 * 
	 */
	@Override
	protected void makeBodyIndexes ( OptionsMap opts )
	{
		var idx = this.getPgIndexer ();
		// TODO: this isn't very open-closed principle, but for the moment it will do.
		if ( idx instanceof SimpleTsvIndexer )
		{
			String outPath = Optional.ofNullable ( opts )
			.map ( o -> o.getString ( "graphmlOutPath" ) )
			.map ( FilenameUtils::removeExtension )
			.orElse ( null );
			
			if ( outPath != null ) {
				var tsvIdx = (SimpleTsvIndexer) idx;
				tsvIdx.setOutputPath ( outPath + "-index.tsv" );
			}
		}
		super.makeBodyIndexes ( opts );
	}

}

package uk.ac.rothamsted.kg.rdf2pg.graphml.export.support;

import static uk.ac.ebi.utils.exceptions.ExceptionUtils.buildEx;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.throwEx;
import static uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLUtils.writeEdgeAttribHeaders;
import static uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLUtils.writeNodeAttribHeaders;
import static uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLUtils.writeXMLAttrib;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.exceptions.ExceptionUtils;
import uk.ac.ebi.utils.exceptions.UncheckedFileNotFoundException;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.AbstractPGDataManager;

/**
 * Utilities to manipilate data for the GraphML output.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>1 Oct 2020</dd></dl>
 *
 */
@Component
public class GraphMLDataManager extends AbstractPGDataManager
{
	public static final String NODE_TMP_FILE_EXTENSION = "-Nodes-tmp"; 
	public static final String EDGE_TMP_FILE_EXTENSION = "-Edges-tmp"; 
	
	private Set<String> gatheredNodeProperties = ConcurrentHashMap.newKeySet ();
	private Set<String> gatheredEdgeProperties = ConcurrentHashMap.newKeySet ();
	
	private String graphmlOutputPath = null;
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	/**
	 * Used to synch file writing operations over file path, see {@link #appendOutput(String, String)}.
	 */
	private Map<String, Writer> outWriters;
	
	{
		gatherNodeProperty ( "iri" );
		gatherNodeProperty ( GraphMLUtils.LABEL_VERTEX_ATTR );
		
		// The iri is built using the md5
		// we then  get the type as well at the same point
		gatherEdgeProperty ( GraphMLUtils.LABEL_EDGE_ATTR );
	}

	
	private void appendOutput ( String graphML, String outPath )
	{		
		try {
			var writer = this.outWriters.get ( outPath );
			writer.append ( graphML );
		}
		catch ( IOException ex )
		{
			ExceptionUtils.throwEx ( 
				UncheckedIOException.class,
				ex,
				"Error while writing to the temp graphML file '%s': %s", outPath, ex.getMessage () 
			);
		}
	}
	
	public void appendNodeOutput ( String graphML )
	{
		appendOutput ( graphML, getNodeTmpPath () );
	}
	
	public void appendEdgeOutput ( String graphML )
	{
		appendOutput ( graphML, getEdgeTmpPath () );
	}
	
	/**
	 * Handlers accumulates all property types during the export, since many graphML readers require to list them
	 * in the file header.
	 */
	public void gatherNodeProperty ( String property ) {
		gatheredNodeProperties.add ( property ); 
	}
	
	public Set<String> getGatheredNodeProperties() {
		return gatheredNodeProperties; 
	}

	
	/**
	 * Handlers accumulates all property types during the export, since many graphML readers require to list them
	 * in the file header.
	 */
	public void gatherEdgeProperty ( String property ) {
		gatheredEdgeProperties.add ( property ); 
	}
	
	public Set<String> getGatheredEdgeProperties () {
		return gatheredEdgeProperties;
	}

	/**
	 * The path to the final output .graphml file
	 */
	public String getGraphmlOutputPath ()
	{
		return graphmlOutputPath;
	}

	public void setGraphmlOutputPath ( String graphmlOutputPath )
	{
		this.graphmlOutputPath = graphmlOutputPath;
		
		Function<String, Writer> wopener = path -> 
		{
			try {
				return new FileWriter ( path );
			}
			catch ( IOException ex ) {
				throw buildEx (
					UncheckedIOException.class, ex, "Error while trying to open temp file \"%s\": %s", path, ex.getMessage () 
				);
			}
		};
		
		outWriters = Stream.of ( getNodeTmpPath (), getEdgeTmpPath () )
		.collect ( 
			Collectors.toMap (
				Function.identity (), 
				outPath -> new BufferedWriter ( wopener.apply ( outPath ), 10 * 2<<19 ) 
		));
	}
	
	private String getNodeTmpPath ()
	{
		return this.graphmlOutputPath + NODE_TMP_FILE_EXTENSION;
	}

	private String getEdgeTmpPath ()
	{
		return this.graphmlOutputPath + EDGE_TMP_FILE_EXTENSION;
	}
	
	
	public void writeGraphML ()
	{
		outWriters.forEach ( (path, writer) -> 
		{
			try {
				writer.close ();
			}
			catch ( IOException ex ) {
				throw buildEx (
					UncheckedIOException.class, ex, "Error while trying to close temp file \"%s\": %s", path, ex.getMessage () 
				);
			}
		});
		
		try ( PrintStream out = new PrintStream (
			new BufferedOutputStream ( 
				new FileOutputStream ( this.graphmlOutputPath ),
				10 * 2<<19
			)
		))
		{
			// Schema headers
			out.println ( GraphMLUtils.GRAPHML_TAG_HEADER );
			
			// Node attribute IDs
			var sb = new StringBuilder ();
			writeNodeAttribHeaders ( this.getGatheredNodeProperties (), sb );
			out.println ( sb.toString () );

			// Relation attribute IDs
			sb = new StringBuilder ();
			writeEdgeAttribHeaders ( this.getGatheredEdgeProperties (), sb );
			out.println( sb.toString()); 
			
			// Opening XML element for the graph
			sb = new StringBuilder(); 
			out.append ( GraphMLUtils.GRAPH_TAG_START );
			writeXMLAttrib ( GraphMLUtils.DEFAULT_DIRECTED_ATTR, GraphMLUtils.DIRECTED_DEFAULT_DIRECTED_VALUE , sb );
			sb.append(" >");
			out.println( sb.toString()); 
	
			
			var l = new ArrayList<String> (); 			
			if (Files.exists(Paths.get(getNodeTmpPath()))) {
				l.add(getNodeTmpPath()); 
			}
			if (Files.exists(Paths.get(getEdgeTmpPath()))) {
				l.add(getEdgeTmpPath()); 
			}
			
			l.stream()
			.forEach ( tempPath -> 
			{
				try ( Reader in = 
					new BufferedReader ( new FileReader ( tempPath, StandardCharsets.UTF_8 ), 2<<19 )
				) 
				{
					// TODO: regain some space by removing the temp at this point
					IOUtils.copy ( in, out, StandardCharsets.UTF_8 );
				}
				catch ( IOException ex ) {
					throw new UncheckedIOException ( String.format ( 
						"I/O error while copying '%s' to '%s': %s", tempPath, graphmlOutputPath ), 
						ex
					);
				}
			});
	
			out.println ( GraphMLUtils.GRAPH_TAG_END );
			out.println ( GraphMLUtils.GRAPHML_TAG_END );
			
			// Clean-up. We don't put it in finally(), cause you typically want to inspect them
			// if an exception occurs
			log.info ( "Deleting temp graphML files" );
			Files.deleteIfExists ( Path.of ( getNodeTmpPath () ) );
			Files.deleteIfExists ( Path.of ( getEdgeTmpPath () ) );
			log.info ( "graphML writing finished" );
		}
		catch ( FileNotFoundException ex ) {
			throwEx ( 
				UncheckedFileNotFoundException.class, ex, 
				"Error while writing to graphML file '%s': %s", this.graphmlOutputPath, ex.getMessage () 
			);
		}
		catch ( IOException ex ) {
			throwEx ( 
				UncheckedIOException.class, ex, 
				"Error while writing to graphML file '%s': %s", this.graphmlOutputPath, ex.getMessage () 
			);
		}
	}	
}

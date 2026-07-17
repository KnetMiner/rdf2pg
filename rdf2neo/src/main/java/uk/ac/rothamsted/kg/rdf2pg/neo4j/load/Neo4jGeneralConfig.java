package uk.ac.rothamsted.kg.rdf2pg.neo4j.load;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.pgmaker.GeneralConfig;

/**
 * A rdf2neo-specific config.
 * 
 * This is loaded and auto-wired in place of the more generic {@link GeneralConfig}, thanks 
 * to the {@link Primary} annotation. This can be further overridden/customised through 
 * the app's bean XML. see the tests for examples.
 * 
 * In particular, the default {@link #getBigValueMode()} is {@link BigValueMode#TRUNCATE_OR_IGNORE},
 * since Neo4j indexing is sensitive to long string and big lists 
 * (<a href="https://neo4j.com/developer/kb/index-limitations-and-workaround/">details</a>).
 *
 * @author Marco Brandizi
 * <dl><dt>Date:</dt><dd>15 Jul 2026</dd></dl>
 *
 */

@Component @Primary
public class Neo4jGeneralConfig extends GeneralConfig
{
	public Neo4jGeneralConfig ()
	{
		super ();
		this.setBigValueMode ( BigValueMode.TRUNCATE_OR_IGNORE );
	}
}

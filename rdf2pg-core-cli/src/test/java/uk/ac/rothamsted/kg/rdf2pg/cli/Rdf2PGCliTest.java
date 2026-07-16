package uk.ac.rothamsted.kg.rdf2pg.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.MultiConfigPGMaker;

/**
 * Test the basic machinery to define/invoke rdf2pg command lines.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>27 Nov 2020</dd></dl>
 *
 */
public class Rdf2PGCliTest
{
	private static final String TEST_CFG_OPTION = "path/to/foo/file.xml";
	private static final String TEST_TDB_OPTION = "path/to/foo/tdb";
	private static final String[] TEST_RDF_OPTION = { "one.ttl", "two.rdf" };
	private static final String TEST_CMD_DESCRIPTION = "A Test Command Line";
	
	private static String cfg, tdb;
	private static String[] rdfs;
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	/**
	 * Of course, this is just a probe to verify the invocations in the tests.
	 */
	@Component
	@Command (
		name = "testCmd", description = TEST_CMD_DESCRIPTION			
	)	
	public static class TestCmd extends Rdf2PgCommand<MultiConfigPGMaker<?, ?>>
	{
		
		public TestCmd ()
		{
			// We don't use it for tests
			super ( null );
		}

		@Override
		public int makePropertyGraph ()
		{
			cfg = this.xmlConfigPath;
			tdb = this.tdbPath;
			rdfs = this.rdfFilePaths;
			
			log.info ( "Command executed, returning 0" );
			return 0;
		}

		@Override
		protected void load2Tdb ()
		{
			log.info ( "Fake TDB loading of: {}", Arrays.toString ( rdfFilePaths ) );
		}
	}
	
	@Test
	public void testCliInvocation ()
	{
		Rdf2PGCli.main ( 
			"--config", TEST_CFG_OPTION, "-t", TEST_TDB_OPTION,
			TEST_RDF_OPTION [ 0 ], TEST_RDF_OPTION [ 1 ]  
		);
		Assert.assertEquals ( "Wrong result from CLI invocation (-c)!", TEST_CFG_OPTION, cfg );
		Assert.assertEquals ( "Wrong result from CLI invocation (-t)!", TEST_TDB_OPTION, tdb );
		Assert.assertEquals ( "Wrong result from CLI invocation (-r)!", 0, Arrays.compare ( TEST_RDF_OPTION, rdfs ) );
		Assert.assertEquals ( "Wrong result from CLI invocation (exit code)!", 0, Rdf2PGCli.getExitCode () );
	}	

	@Test
	public void testCliHelp ()
	{
		// Capture the output into memory
		PrintStream outBkp = System.out;
		ByteArrayOutputStream outBuf = new ByteArrayOutputStream ();
		System.setOut ( new PrintStream ( outBuf ) );

		Rdf2PGCli.main ( "-h" );
		
		System.setOut ( outBkp );  // restore the original output

		log.debug ( "CLI output:\n{}", outBuf.toString () );
		assertTrue ( "Can't find CLI usage output (--config)!", outBuf.toString ().contains ( "--config" ) );
		assertTrue ( "Can't find CLI usage output (--tdb)!", outBuf.toString ().contains ( "--tdb" ) );
		assertTrue ( "Can't find CLI usage output (description)!", outBuf.toString ().contains ( TEST_CMD_DESCRIPTION ) );
		
		assertEquals ( "Wrong exit code from --help invocation!", ExitCode.USAGE, Rdf2PGCli.getExitCode () );
	}	

}

package uk.ac.rothamsted.neo4j.utils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Values;
import org.neo4j.driver.reactivestreams.ReactiveResult;
import org.neo4j.driver.reactivestreams.ReactiveTransactionContext;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.AfterClass;

/**
 *
 * @author Marco Brandizi
 * <dl><dt>Date:</dt><dd>7 Oct 2024</dd></dl>
 *
 */
public class Neo4jUtilsIT
{
	// TODO: duplicated from rdf2neo, factorise
	
	public static final String NEO_TEST_URL = "bolt://127.0.0.1:" + System.getProperty ( "neo4j.server.boltPort" );
	public static final String NEO_TEST_USER = "neo4j";
	public static final String NEO_TEST_PWD = "testTest";
	
	private static Driver neoDriver = GraphDatabase.driver ( 
		NEO_TEST_URL, AuthTokens.basic ( NEO_TEST_USER, NEO_TEST_PWD )
	);
	
	private static int testNodesSize = 1000;
	
	@BeforeClass
	public static void init ()
	{
		try ( var session = neoDriver.session () )
		{
			session.executeWriteWithoutResult ( tx ->
			{ 
				for ( int i = 0; i < testNodesSize; i++ )
					tx.run ( 
						"MATCH (n:PagerTestNode) DELETE n"
					);				
			});

			session.executeWriteWithoutResult ( tx ->
			{ 
				for ( int i = 0; i < testNodesSize; i++ )
					tx.run ( 
						"CREATE (:PagerTestNode { idx: $idx })",
						Values.parameters ( "idx", i ) 
					);				
			});
		}
	}
	
	
	@AfterClass
	public static void close ()
	{		
		neoDriver.close ();
	}

	
	@Test
	public void testPaginatedRead ()
	{
		long pageSize = 20;
		var pager = Neo4jUtils.paginatedRead (
			(tx, offset) -> tx.run (
				"MATCH ( n: PagerTestNode ) RETURN n.idx AS idx SKIP $offset LIMIT $pageSize",
				Values.parameters ( "offset", offset, "pageSize", pageSize )
			), 
			neoDriver,
			pageSize
		);
		
		int i = 0;
		while ( pager.hasNext () )
			assertEquals ( "Wrong item fetched!", i++, pager.next ().get ( "idx", -1 ) );
		
		assertEquals ( "Wrong size fetched!", testNodesSize, i );
	}
	
	@Test
	public void testPaginatedReadEmpty ()
	{
		long pageSize = 20;
		var pager = Neo4jUtils.paginatedRead (
			(tx, offset) -> tx.run (
				"MATCH ( n: PagerTestNodeFoo ) RETURN n.idx AS idx SKIP $offset LIMIT $pageSize",
				Values.parameters ( "offset", offset, "pageSize", pageSize )
			), 
			neoDriver,
			pageSize
		);
		
		Assert.assertFalse ( "pager should be false against empty Cypher!", pager.hasNext () );
	}

	@Test
	public void testPaginatedReadSinglePage ()
	{
		long pageSize = testNodesSize;
		Iterator<Integer> pager = Neo4jUtils.paginatedRead (
			(tx, offset) -> tx.run (
				"MATCH ( n: PagerTestNode ) RETURN n.idx AS idx SKIP $offset LIMIT $pageSize",
				Values.parameters ( "offset", offset, "pageSize", pageSize )
			).stream ()
			.map ( r -> r.get ( "idx", -1 ) )
			.iterator (), 
			neoDriver,
			pageSize
		);
		
		int i = 0;
		while ( pager.hasNext () )
			assertEquals ( "Wrong item fetched (single page query)!", i++, pager.next ().intValue () );
		
		assertEquals ( "Wrong size fetched (single page query)!", testNodesSize, i );
	}
	
	
	@Test
	public void testReactivePaginatedRead ()
	{
		long pageSize = 20;
				 
		// In many cases, you can call this version, which spares you the conversion to
		// a Flux of Records. See the other test methods for a lower-level version. 
		//
		Flux<Integer> pagerFlux = Neo4jUtils.reactivePaginatedRead2Records (
			(tx, offset) -> tx.run ( 
				"MATCH ( n: PagerTestNode ) RETURN n.idx AS idx SKIP $offset LIMIT $pageSize",
				Values.parameters ( "offset", offset, "pageSize", pageSize )
			),
			neoDriver,
			pageSize
		)
		.map ( r -> r.get ( "idx", -1 ) );
		
		Iterator<Integer> pager = pagerFlux.toIterable ().iterator ();
		
		int i = 0;
		while ( pager.hasNext () )
			assertEquals ( "Wrong item fetched!", i++, pager.next ().intValue () );
		
		assertEquals ( "Wrong size fetched!", testNodesSize, i );
	}
	
	
	@Test
	public void testReactivePaginatedReadEmpty ()
	{
		long pageSize = 20;
				 
		// As mentioned above, this is a lower-level version, which might be useful eg, to 
		// call ReactiveResult.consume() or .keys()
		//
		Flux<Integer> pagerFlux = Neo4jUtils.reactivePaginatedRead (
			(tx, offset) -> Mono.fromDirect ( tx.run ( 
				"MATCH ( n: PagerTestNodeFoo ) RETURN n.idx AS idx SKIP $offset LIMIT $pageSize",
				Values.parameters ( "offset", offset, "pageSize", pageSize )
			))
			.flatMapMany ( ReactiveResult::records )
			.map ( r -> r.get ( "idx", -1 ) ),
			neoDriver,
			pageSize
		);
		
		Iterator<Integer> pager = pagerFlux.toIterable ().iterator ();
		Assert.assertFalse ( "pager should be false against empty Cypher!", pager.hasNext () );
	}
	
	
	@Test
	public void testReactivePaginatedReadSinglePage ()
	{
		long pageSize = testNodesSize;
				 
		Flux<Integer> pagerFlux = Neo4jUtils.reactivePaginatedRead2Records (
			(tx, offset) -> tx.run ( 
				"MATCH ( n: PagerTestNode ) RETURN n.idx AS idx SKIP $offset LIMIT $pageSize",
				Values.parameters ( "offset", offset, "pageSize", pageSize )
			)
			,
			neoDriver,
			pageSize
		)
		.map ( r -> r.get ( "idx", -1 ) );
		
		Iterator<Integer> pager = pagerFlux.toIterable ().iterator ();
		
		int i = 0;
		while ( pager.hasNext () )
			assertEquals ( "Wrong item fetched!", i++, pager.next ().intValue () );
		
		assertEquals ( "Wrong size fetched!", testNodesSize, i );
	}
	
}

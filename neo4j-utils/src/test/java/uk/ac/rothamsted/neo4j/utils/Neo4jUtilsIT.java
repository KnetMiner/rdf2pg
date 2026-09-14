package uk.ac.rothamsted.neo4j.utils;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.Values;
import org.neo4j.driver.reactivestreams.ReactiveResult;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.ac.rothamsted.neo4j.utils.test.NeoTestContainerResource;

/**
 *
 * @author Marco Brandizi
 * <dl><dt>Date:</dt><dd>7 Oct 2024</dd></dl>
 *
 */
public class Neo4jUtilsIT
{		
	private static int testNodesSize = 1000;
	
	@ClassRule
	public static NeoTestContainerResource neoContainer = new NeoTestContainerResource ();
	
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger ( Neo4jUtilsIT.class );

	
	@BeforeClass
	public static void init ()
	{		
		try ( var session = neoContainer.getNeoDriver ().session () )
		{
			/* TODO: remove, no longer needed with TestContainers

			session.executeWriteWithoutResult ( tx ->
			{ 
				for ( int i = 0; i < testNodesSize; i++ )
					tx.run ( 
						"MATCH (n:PagerTestNode) DELETE n"
					);				
			});
			
			*/

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
	
	
	@Test
	public void testPaginatedRead ()
	{
		long pageSize = 20;
		var pager = Neo4jUtils.paginatedRead (
			(tx, offset) -> tx.run (
				"MATCH ( n: PagerTestNode ) RETURN n.idx AS idx SKIP $offset LIMIT $pageSize",
				Values.parameters ( "offset", offset, "pageSize", pageSize )
			), 
			neoContainer.getNeoDriver (),
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
			neoContainer.getNeoDriver (),
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
			neoContainer.getNeoDriver (),
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
			neoContainer.getNeoDriver (),
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
			neoContainer.getNeoDriver (),
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
			neoContainer.getNeoDriver (),
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
